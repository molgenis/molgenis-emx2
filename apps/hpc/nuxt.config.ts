export default defineNuxtConfig({
  extends: ["../tailwind-components"],
  ssr: false,
  tailwindcss: {
    cssPath: "../tailwind-components/app/assets/css/main.css",
    configPath: "../tailwind-components/tailwind.config.js",
  },
  runtimeConfig: {
    public: {
      apiBase: "http://localhost:8080/",
    },
  },
  nitro: {
    devProxy: {
      "/theme.css": {
        target: "http://localhost:8080/_SYSTEM_/theme.css",
        changeOrigin: true,
      },
    },
    prerender: { ignore: ["/_tailwind/"] },
  },
});
