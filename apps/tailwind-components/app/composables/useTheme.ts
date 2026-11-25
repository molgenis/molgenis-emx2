import { useCookie, useHead, useState } from "#app";
import { watch } from "vue";
import type { ThemeMode } from "../../types/types";

/**
 * The header can render more than one theme switch (one per screen size), so
 * the active theme lives in shared state and every switch stays in sync.
 */
export function useTheme() {
  const themeCookie = useCookie<ThemeMode>("theme", {
    default: () => {
      if (import.meta.client) {
        return window.matchMedia("(prefers-color-scheme: dark)").matches
          ? "dark"
          : "light";
      }
      return "light";
    },
    maxAge: 60 * 60 * 24 * 365, // 1 year
    sameSite: "lax",
    path: "/",
  });

  const activeTheme = useState<ThemeMode>("theme", () => themeCookie.value);

  watch(activeTheme, (theme) => {
    themeCookie.value = theme;
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
