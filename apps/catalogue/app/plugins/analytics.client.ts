import { defineNuxtPlugin, useCookie } from "#imports";
import { setupAnalytics } from "../../../emx2-analytics/src/lib/analytics";
import type {
  Provider,
  providerId,
} from "../../../emx2-analytics/src/types/Provider";

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook("page:loading:end", () => {
    const schema = nuxtApp.$config.public.schema as string;
    const analyticsKey = nuxtApp.$config.public.analyticsKey;
    const analyticsProvider = nuxtApp.$config.public
      .analyticsProvider as providerId;
    const isAnalyticsAllowedCookie = useCookie("mg_allow_analytics");
    if (
      schema &&
      analyticsKey &&
      analyticsProvider &&
      isAnalyticsAllowedCookie.value === "true"
    ) {
      console.log("Setup Analytics for: " + schema);
      const providers: Provider[] = [
        { id: analyticsProvider, options: { analyticsKey } },
      ];
      setupAnalytics(schema, providers);
    }
  });
});
