import { UserManager, WebStorageStateStore } from 'oidc-client-ts';

export const CALLBACK_PATH = '/app/login/callback';
export const LOGIN_PATH = '/app/login';
export const DEFAULT_RETURN_PATH = '/app/';

function requiredEnv(name) {
  const value = import.meta.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

let manager;
let cachedAccessToken;
let redirectCallback;

function rememberToken(user) {
  cachedAccessToken = user && !user.expired ? user.access_token : undefined;
}

export function getUserManager() {
  if (!manager) {
    const url = requiredEnv('VITE_KEYCLOAK_URL');
    const realm = requiredEnv('VITE_KEYCLOAK_REALM');
    manager = new UserManager({
      authority: `${url}/realms/${realm}`,
      client_id: requiredEnv('VITE_KEYCLOAK_CLIENT_ID'),
      redirect_uri: `${window.location.origin}${CALLBACK_PATH}`,
      post_logout_redirect_uri: `${window.location.origin}${LOGIN_PATH}`,
      scope: 'openid profile email',
      userStore: new WebStorageStateStore({ store: window.sessionStorage }),
      automaticSilentRenew: true,
      monitorSession: false,
    });

    manager.events.addUserLoaded(loadedUser => rememberToken(loadedUser));
    manager.events.addUserUnloaded(() => rememberToken(null));
    manager.events.addSilentRenewError(() => rememberToken(null));
  }
  return manager;
}

export function safeReturnPath(state) {
  const isLocalPath =
    typeof state === 'string' &&
    state.startsWith('/') &&
    !state.startsWith('//');
  if (isLocalPath) {
    return state;
  }
  return DEFAULT_RETURN_PATH;
}

export function completeSigninRedirect() {
  if (!redirectCallback) {
    redirectCallback = getUserManager()
      .signinRedirectCallback()
      .then(signedIn => {
        rememberToken(signedIn);
        return safeReturnPath(signedIn.state);
      });
  }
  return redirectCallback;
}

export async function getOidcUser() {
  const user = await getUserManager().getUser();
  rememberToken(user);
  if (!user || user.expired) {
    return null;
  }
  return user;
}

export async function hasOidcSession() {
  return Boolean(await getOidcUser());
}

export function getCachedAccessToken() {
  return cachedAccessToken;
}

export async function getAccessToken() {
  let user = await getUserManager().getUser();

  if (!user || user.expired) {
    try {
      user = await getUserManager().signinSilent();
    } catch {
      await getUserManager().removeUser();
      user = null;
    }
  }

  rememberToken(user);
  return user?.access_token;
}

export function attachAuthInterceptor(client) {
  client.interceptors.request.use(async config => {
    const token = await getAccessToken();
    if (!token) {
      return Promise.reject(new Error('Session expired'));
    }
    config.headers.Authorization = `Bearer ${token}`;
    return config;
  });

  client.interceptors.response.use(
    response => response,
    async error => {
      if (error?.response?.status === 401) {
        await getUserManager().removeUser();
        rememberToken(null);
      }
      return Promise.reject(error);
    }
  );
}
