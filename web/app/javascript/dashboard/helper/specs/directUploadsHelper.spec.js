import { setDirectUploadAuthHeaders } from '../directUploadsHelper';
import { getCachedAccessToken } from 'shared/auth/oidc';

vi.mock('shared/auth/oidc', () => ({
  getCachedAccessToken: vi.fn(),
}));

describe('setDirectUploadAuthHeaders', () => {
  const buildXhr = () => ({ setRequestHeader: vi.fn() });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('sets a Bearer token from the OIDC session', () => {
    getCachedAccessToken.mockReturnValue('token-123');
    const xhr = buildXhr();

    setDirectUploadAuthHeaders(xhr);

    expect(xhr.setRequestHeader).toHaveBeenCalledTimes(1);
    expect(xhr.setRequestHeader).toHaveBeenCalledWith(
      'Authorization',
      'Bearer token-123'
    );
  });

  it('does not set any header when there is no access token', () => {
    getCachedAccessToken.mockReturnValue(undefined);
    const xhr = buildXhr();

    setDirectUploadAuthHeaders(xhr);

    expect(xhr.setRequestHeader).not.toHaveBeenCalled();
  });
});
