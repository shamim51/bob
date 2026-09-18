import { attachAuthInterceptor } from 'shared/auth/oidc';
import { API_HOST } from 'shared/config/apiHost';

const parseErrorCode = error => Promise.reject(error);

export default axios => {
  const wootApi = axios.create({ baseURL: `${API_HOST}/` });
  attachAuthInterceptor(wootApi);
  wootApi.interceptors.response.use(
    response => response,
    error => parseErrorCode(error)
  );
  return wootApi;
};
