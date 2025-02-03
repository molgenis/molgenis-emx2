import { defineNuxtPlugin } from "#imports";
import { setupAnalytics } from "@molgenis/emx2-analytics";

export default defineNuxtPlugin((nuxtApp) => {
  nuxtApp.hook("page:loading:end", () => {
    const schema = nuxtApp._route.params["schema"] as string;
    const analyticsKey = nuxtApp.$config.public.analyticsKey;
    if (schema && analyticsKey) {
      console.log("Setup Analytics for: " + schema);
      const providers = [{ id: "site-improve", options: { analyticsKey } }];
      setupAnalytics(schema, providers);
    }
  });
});
