import path from 'path';
import { fileURLToPath } from 'url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import yaml from '@rollup/plugin-yaml';

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

const vueOptions = {
  template: {
    compilerOptions: {
      isCustomElement: tag => ['ninja-keys'].includes(tag),
    },
  },
};

function rewriteToPack(urlPath) {
  const pathname = urlPath.split('?')[0];
  if (pathname.includes('.') && !pathname.endsWith('.html')) {
    return null;
  }
  if (pathname.includes('/login') || pathname.includes('/auth')) {
    return '/login.html';
  }
  if (pathname === '/' || pathname.startsWith('/app')) {
    return '/index.html';
  }
  return null;
}

function chatwootHistoryFallback() {
  const apply = server => {
    server.middlewares.use((req, _res, next) => {
      if (req.method !== 'GET' && req.method !== 'HEAD') {
        next();
        return;
      }
      const rewritten = rewriteToPack(req.url || '/');
      if (rewritten) {
        req.url = rewritten;
      }
      next();
    });
  };

  return {
    name: 'chatwoot-history-fallback',
    configureServer(server) {
      return () => apply(server);
    },
    configurePreviewServer(server) {
      return () => apply(server);
    },
  };
}

export default defineConfig({
  appType: 'mpa',
  plugins: [vue(vueOptions), yaml(), chatwootHistoryFallback()],
  css: {
    preprocessorOptions: {
      scss: {
        api: 'modern-compiler',
      },
    },
  },
  resolve: { alias: aliases },
  publicDir: 'public',
  build: {
    rollupOptions: {
      input: {
        dashboard: path.resolve(root, 'index.html'),
        login: path.resolve(root, 'login.html'),
      },
    },
  },
  server: {
    port: 5173,
    strictPort: false,
  },
});
