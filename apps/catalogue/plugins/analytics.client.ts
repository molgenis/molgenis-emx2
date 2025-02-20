import { defineNuxtPlugin } from "#imports";
import { setupAnalytics } from "../../emx2-analytics/src/lib/analytics";
import type { Provider } from "../../emx2-analytics/src/types/Provider";

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook("page:loading:end", () => {
    const schema = nuxtApp._route.params["schema"] as string;
    const analyticsKey = nuxtApp.$config.public.analyticsKey;
    if (schema && analyticsKey) {
      console.log("Setup Analytics for: " + schema);
      const providers: Provider[] = [
        { id: "site-improve", options: { analyticsKey } },
      ];
      setupAnalytics(schema, providers);
    }
  });
});
