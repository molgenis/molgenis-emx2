import { useRoute } from "nuxt/app";
import { fetchErrorToNuxtError } from "../utils/fetchErrorToNuxtError";
import { computed, ref } from "vue";
import type { RouteLocationNormalizedGeneric } from "vue-router";
import type { Resp, Settings } from "../../types/types";

export const useSchemaSettings = async (
  keys: Set<string>,
  middlewareRoute?: RouteLocationNormalizedGeneric
) => {
  // middlewareRoute is used to get the route in middleware, where useRoute() cannot be used (see: https://nuxt.com/docs/4.x/directory-structure/app/middleware#accessing-route-in-middleware)
  const route = middlewareRoute ?? useRoute();
  const schema = computed(() =>
    Array.isArray(route.params.schema)
      ? route.params.schema[0]
      : route.params.schema
  );
  if (schema.value) {
    const response = await $fetch<
      Resp<{ _settings: { key: string; value: string }[] }>
    >(`/${schema.value}/graphql`, {
      method: "POST",
      body: JSON.stringify({
        query: `{_settings (keys: [${Array.from(keys)
          .map((k) => `"${k}"`)
          .join(",")}]){ key, value }}`,
      }),
    }).catch((error) => {
      console.error("Error fetching schema settings", error);
      throw fetchErrorToNuxtError(
        error,
        `Could not fetch schema settings for schema ${schema.value}.`
      );
    });

    if (response) {
      const settingsArray: { key: string; value: string }[] = Array.isArray(
        response.data
      )
        ? response.data[0]._settings
        : response.data._settings;

      const settingsMap = ref<Settings>({});
      settingsArray.forEach((item) => {
        settingsMap.value[item.key] = item.value;
      });

      return settingsMap;
    }
  }
};
