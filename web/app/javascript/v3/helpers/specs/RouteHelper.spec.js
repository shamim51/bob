import { validateRouteAccess, isOnOnboardingView } from '../RouteHelper';
import { replaceRouteWithReload } from '../CommonHelper';
import { hasOidcSession } from 'shared/auth/oidc';

const next = vi.fn();
vi.mock('../CommonHelper', () => ({ replaceRouteWithReload: vi.fn() }));
vi.mock('shared/auth/oidc', () => ({
  hasOidcSession: vi.fn(),
}));

describe('#validateRouteAccess', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('ignore session and continue to the page if the ignoreSession is present in route definition', async () => {
    await validateRouteAccess(
      {
        name: 'login',
        meta: { ignoreSession: true },
      },
      next
    );
    expect(hasOidcSession).not.toHaveBeenCalled();
    expect(next).toHaveBeenCalledTimes(1);
  });

  it('redirects to dashboard if an OIDC session is present', async () => {
    hasOidcSession.mockResolvedValueOnce(true);

    await validateRouteAccess({ name: 'login' }, next);
    expect(replaceRouteWithReload).toHaveBeenCalledWith('/app/');
    expect(next).not.toHaveBeenCalled();
  });

  it('redirects to login if route is empty', async () => {
    hasOidcSession.mockResolvedValueOnce(false);

    await validateRouteAccess({}, next);
    expect(next).toHaveBeenCalledWith('/app/login');
  });

  it('continues to the route in every other case', async () => {
    hasOidcSession.mockResolvedValueOnce(false);

    await validateRouteAccess({ name: 'login' }, next);
    expect(next).toHaveBeenCalledWith();
  });
});

describe('isOnOnboardingView', () => {
  test('returns true for a route with onboarding name', () => {
    const route = { name: 'onboarding_welcome' };
    expect(isOnOnboardingView(route)).toBe(true);
  });

  test('returns false for a route without onboarding name', () => {
    const route = { name: 'home' };
    expect(isOnOnboardingView(route)).toBe(false);
  });

  test('returns false for a route with null name', () => {
    const route = { name: null };
    expect(isOnOnboardingView(route)).toBe(false);
  });

  test('returns false for an  undefined route object', () => {
    expect(isOnOnboardingView()).toBe(false);
  });
});
