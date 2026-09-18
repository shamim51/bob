import path from 'path';
import { fileURLToPath } from 'url';

const root = path.dirname(fileURLToPath(import.meta.url));

export const aliases = {
  vue: 'vue/dist/vue.esm-bundler.js',
  components: path.resolve(root, 'app/javascript/dashboard/components'),
  next: path.resolve(root, 'app/javascript/dashboard/components-next'),
  v3: path.resolve(root, 'app/javascript/v3'),
  dashboard: path.resolve(root, 'app/javascript/dashboard'),
  helpers: path.resolve(root, 'app/javascript/shared/helpers'),
  shared: path.resolve(root, 'app/javascript/shared'),
  widget: path.resolve(root, 'app/javascript/widget'),
  assets: path.resolve(root, 'app/javascript/dashboard/assets'),
};

export const vueOptions = {
  template: {
    compilerOptions: {
      isCustomElement: (tag: string) => ['ninja-keys'].includes(tag),
    },
  },
};
