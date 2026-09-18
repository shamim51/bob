<script>
import { mapGetters } from 'vuex';
import { useBranding } from 'shared/composables/useBranding';
import { getUserManager, hasOidcSession } from 'shared/auth/oidc';
import { DEFAULT_REDIRECT_URL } from 'dashboard/constants/globals';
import NextButton from 'dashboard/components-next/button/Button.vue';

export default {
  components: {
    NextButton,
  },
  setup() {
    const { replaceInstallationName } = useBranding();
    return {
      replaceInstallationName,
    };
  },
  data() {
    return {
      signingIn: false,
      error: '',
    };
  },
  computed: {
    ...mapGetters({ globalConfig: 'globalConfig/get' }),
  },
  async created() {
    if (await hasOidcSession()) {
      window.location.replace(DEFAULT_REDIRECT_URL);
    }
  },
  methods: {
    handleSignIn() {
      this.error = '';
      this.signingIn = true;
      getUserManager()
        .signinRedirect({ state: DEFAULT_REDIRECT_URL })
        .catch(caught => {
          // eslint-disable-next-line no-console
          console.error('Sign-in failed', caught);
          this.error = this.$t('LOGIN.SIGN_IN_FAILED');
          this.signingIn = false;
        });
    },
  },
};
</script>

<template>
  <main
    class="flex flex-col w-full min-h-screen py-20 bg-n-brand/5 dark:bg-n-background sm:px-6 lg:px-8"
  >
    <section class="max-w-5xl mx-auto">
      <img
        :src="globalConfig.logo"
        :alt="globalConfig.installationName"
        class="block w-auto h-8 mx-auto dark:hidden"
      />
      <img
        v-if="globalConfig.logoDark"
        :src="globalConfig.logoDark"
        :alt="globalConfig.installationName"
        class="hidden w-auto h-8 mx-auto dark:block"
      />
      <h2 class="mt-6 text-3xl font-medium text-center text-n-slate-12">
        {{ replaceInstallationName($t('LOGIN.TITLE')) }}
      </h2>
      <p class="mt-3 text-sm text-center text-n-slate-11">
        {{ $t('LOGIN.KEYCLOAK_HINT') }}
      </p>
    </section>

    <section
      class="bg-white shadow sm:mx-auto mt-11 sm:w-full sm:max-w-lg dark:bg-n-solid-2 p-11 sm:shadow-lg sm:rounded-lg"
    >
      <p v-if="error" class="mb-4 text-sm text-center text-n-ruby-11">
        {{ error }}
      </p>
      <NextButton
        lg
        type="button"
        data-testid="submit_button"
        class="w-full"
        :label="signingIn ? $t('LOGIN.SIGNING_IN') : $t('LOGIN.SUBMIT')"
        :disabled="signingIn"
        :is-loading="signingIn"
        @click="handleSignIn"
      />
    </section>
  </main>
</template>
