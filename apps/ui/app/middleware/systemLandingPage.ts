import { defineNuxtRouteMiddleware, navigateTo } from "#app";
import { useSettings } from "../../../tailwind-components/app/composables/useSettings";
import { parseLinkSetting } from "../../../tailwind-components/app/utils/typeUtils";

const SYSTEM_LANDING_PAGE = "SYSTEM_LANDING_PAGE";

// Navigate to location specified in system settings.SYSTEM_LANDING_PAGE if it exists
export default defineNuxtRouteMiddleware(async () => {
  const setting = await useSettings(new Set([SYSTEM_LANDING_PAGE]));

  if (setting.value?.SYSTEM_LANDING_PAGE) {
    const link = parseLinkSetting(setting.value.SYSTEM_LANDING_PAGE as string);
    return await navigateTo(link.link, {
      replace: true,
      external: !link.isSpaLink,
    });
  }
});
