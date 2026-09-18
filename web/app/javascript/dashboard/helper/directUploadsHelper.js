import { getCachedAccessToken } from 'shared/auth/oidc';

export const setDirectUploadAuthHeaders = xhr => {
  const accessToken = getCachedAccessToken();
  if (!accessToken) return;

  xhr.setRequestHeader('Authorization', `Bearer ${accessToken}`);
};
