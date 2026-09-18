import axios from 'axios';
import { attachAuthInterceptor } from 'shared/auth/oidc';
import { API_HOST } from 'shared/config/apiHost';

const wootAPI = axios.create({ baseURL: `${API_HOST}/` });
attachAuthInterceptor(wootAPI);

export default wootAPI;
