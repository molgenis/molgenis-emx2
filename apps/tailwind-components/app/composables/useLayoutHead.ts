import { useHead, tryUseNuxtApp } from "#app";
import { computed } from "vue";
import type { RouteLocationNormalizedLoaded } from "vue-router";
import { useConfiguredTheme } from "./useConfiguredTheme";

export function useLayoutHead(route: RouteLocationNormalizedLoaded) {
  const nuxtApp = tryUseNuxtApp();
  if (!nuxtApp) {
    return;
  }

  const config = nuxtApp.$config;
  const faviconHref = config.public.emx2Theme
    ? `/_nuxt-styles/img/${config.public.emx2Theme}.ico`
    : "/_nuxt-styles/img/molgenis.ico";

  const configuredTheme = useConfiguredTheme();
  const dataTheme = computed(
    () =>
      (route.query.theme as string) ||
      configuredTheme.value?.base ||
      (config.public.emx2Theme as string) ||
      ""
  );

  useHead({
    htmlAttrs: {
      "data-theme": dataTheme,
    },
    link: [{ rel: "icon", href: faviconHref }],
    titleTemplate: (titleChunk: string | undefined): string | null => {
      if (titleChunk && config.public.siteTitle) {
        return `${titleChunk} | ${config.public.siteTitle}`;
      } else if (titleChunk) {
        return titleChunk;
      } else if (config.public.siteTitle) {
        return config.public.siteTitle as string;
      } else {
        return "Emx2";
      }
    },
  });
}
