<script>
import { completeSigninRedirect, LOGIN_PATH } from 'shared/auth/oidc';
import Spinner from 'shared/components/Spinner.vue';

export default {
  components: {
    Spinner,
  },
  async created() {
    try {
      const next = await completeSigninRedirect();
      window.location.replace(next);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.error('Failed to complete sign-in', error);
      window.location.replace(LOGIN_PATH);
    }
  },
};
</script>

<template>
  <main
    class="flex flex-col items-center justify-center w-full min-h-screen bg-n-brand/5 dark:bg-n-background"
  >
    <Spinner color-scheme="primary" size="" />
  </main>
</template>
