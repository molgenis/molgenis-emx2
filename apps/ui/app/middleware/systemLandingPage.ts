import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSettings } from "../../../tailwind-components/app/composables/useSettings";
import {
  parseLinkSetting,
  resolveLinkLocation,
} from "../../../tailwind-components/app/utils/typeUtils";

const SYSTEM_LANDING_PAGE = "SYSTEM_LANDING_PAGE";

// Navigate to location specified in system settings.SYSTEM_LANDING_PAGE if it exists
export default defineNuxtRouteMiddleware(async () => {
  const setting = await useSettings(new Set([SYSTEM_LANDING_PAGE]));

  if (setting.value?.SYSTEM_LANDING_PAGE) {
    const link = parseLinkSetting(setting.value.SYSTEM_LANDING_PAGE as string);
    const location = resolveLinkLocation(link);
    if (location) {
      return await navigateTo(location, {
        replace: true,
        external: link.linkType !== "spaLink",
      });
    }
  }
});
