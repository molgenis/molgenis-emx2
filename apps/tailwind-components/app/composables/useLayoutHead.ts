import { useHead, tryUseNuxtApp } from "#app";
import type { RouteLocationNormalizedLoaded } from "vue-router";
import { useActiveUiTheme } from "./useUiTheme";

export function useLayoutHead(route: RouteLocationNormalizedLoaded) {
  const nuxtApp = tryUseNuxtApp();
  if (!nuxtApp) {
    return;
  }

  const config = nuxtApp.$config;
  const faviconHref = config.public.emx2Theme
    ? `/_nuxt-styles/img/${config.public.emx2Theme}.ico`
    : "/_nuxt-styles/img/molgenis.ico";

  const uiTheme = useActiveUiTheme();

  useHead({
    htmlAttrs: {
      "data-theme": () =>
        (route.query.theme as string) ||
        uiTheme.value ||
        (config.public.emx2Theme as string) ||
        "",
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
