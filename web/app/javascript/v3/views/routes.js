import { frontendURL } from 'dashboard/helper/URLHelper';

import Login from './login/Index.vue';
import Callback from './login/Callback.vue';

export default [
  {
    path: frontendURL('login'),
    name: 'login',
    component: Login,
  },
  {
    path: frontendURL('login/callback'),
    name: 'login_callback',
    component: Callback,
    meta: { ignoreSession: true },
  },
  {
    path: frontendURL('login/sso'),
    redirect: { name: 'login' },
  },
  {
    path: frontendURL('auth/signup'),
    redirect: { name: 'login' },
  },
  {
    path: frontendURL('auth/confirmation'),
    redirect: { name: 'login' },
  },
  {
    path: frontendURL('auth/verify-email'),
    redirect: { name: 'login' },
  },
  {
    path: frontendURL('auth/password/edit'),
    redirect: { name: 'login' },
  },
  {
    path: frontendURL('auth/reset/password'),
    redirect: { name: 'login' },
  },
];
