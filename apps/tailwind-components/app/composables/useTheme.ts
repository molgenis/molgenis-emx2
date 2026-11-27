import { useCookie, useHead, useState } from "#app";
import { computed, watch } from "vue";
import type { ThemeMode } from "../../types/types";
import { TOGGLEABLE_THEMES, useActiveUiTheme } from "./useUiTheme";

/**
 * The header can render more than one theme switch (one per screen size), so
 * the user's choice lives in shared state and every switch stays in sync.
 */
export function useTheme() {
  const uiTheme = useActiveUiTheme();
  // the cookie only holds an explicit user choice, without one we fall back to
  // the (per schema) ui theme, computed so it follows route changes
  const themeCookie = useCookie<ThemeMode | null>("theme", {
    maxAge: 60 * 60 * 24 * 365, // 1 year
    sameSite: "lax",
    path: "/",
  });

  const chosenTheme = useState<ThemeMode | null>(
    "theme",
    () => themeCookie.value ?? null
  );

  watch(chosenTheme, (theme) => {
    themeCookie.value = theme;
  });

  const activeTheme = computed<ThemeMode>({
    get: () => {
      if (chosenTheme.value) {
        return chosenTheme.value;
      } else if (TOGGLEABLE_THEMES.includes(uiTheme.value)) {
        return uiTheme.value as ThemeMode;
      } else if (import.meta.client) {
        return window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light";
      } else {
        return "light";
      }
    },
    set: (value) => {
      chosenTheme.value = value;
    },
  });

  useHead({
    htmlAttrs: {
      "data-theme": activeTheme,
    },
  });

  function toggleTheme() {
    activeTheme.value = activeTheme.value === "light" ? "dark" : "light";
  }

  return { activeTheme, toggleTheme };
}
