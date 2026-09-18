import { frontendURL } from 'dashboard/helper/URLHelper';
import { hasOidcSession } from 'shared/auth/oidc';
import { DEFAULT_REDIRECT_URL } from 'dashboard/constants/globals';
import { replaceRouteWithReload } from './CommonHelper';

export const validateRouteAccess = async (to, next) => {
  if (to.meta && to.meta.ignoreSession) {
    next();
    return;
  }

  if (await hasOidcSession()) {
    replaceRouteWithReload(DEFAULT_REDIRECT_URL);
    return;
  }

  if (!to.name) {
    next(frontendURL('login'));
    return;
  }

  next();
};

export const isOnOnboardingView = route => {
  const { name = '' } = route || {};

  if (!name) {
    return false;
  }

  return name.includes('onboarding_');
};
