import { useState } from "#app/composables/state";
import type { IConfiguredTheme } from "../utils/themes";

// the theme an admin or manager configured; null means nothing is configured
export const useConfiguredTheme = () =>
  useState<IConfiguredTheme | null>("configuredTheme", () => null);
