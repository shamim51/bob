import en from './locale/en';

const localeLoaders = import.meta.glob('./locale/*/index.js');

export const supportedLocales = Object.keys(localeLoaders)
  .map(path => path.match(/\/locale\/([^/]+)\//)?.[1])
  .filter(Boolean);

export default { en };

export const normalizeLocale = locale => {
  if (!locale) return 'en';
  return String(locale).replace(/-/g, '_');
};

export const loadLocaleMessages = async locale => {
  const code = normalizeLocale(locale);
  if (code === 'en') return en;

  const loader = localeLoaders[`./locale/${code}/index.js`];
  if (!loader) return en;

  const mod = await loader();
  return mod.default;
};

export const applyLocale = async (i18n, locale) => {
  const global = i18n?.global || i18n;
  if (!global) return;

  const requested = normalizeLocale(locale);
  const loader = localeLoaders[`./locale/${requested}/index.js`];
  const code = requested === 'en' || loader ? requested : 'en';

  const availableLocales = global.availableLocales;
  const available = Array.isArray(availableLocales)
    ? availableLocales
    : availableLocales?.value || [];
  if (!available.includes(code)) {
    try {
      const messages = await loadLocaleMessages(code);
      global.setLocaleMessage(code, messages);
    } catch {
      if (global.locale && typeof global.locale === 'object') {
        global.locale.value = 'en';
      } else {
        global.locale = 'en';
      }
      return;
    }
  }

  if (global.locale && typeof global.locale === 'object') {
    global.locale.value = code;
  } else {
    global.locale = code;
  }
};
