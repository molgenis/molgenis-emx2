import { useRoute } from "#app";
import { computed } from "vue";
import type { Resp } from "../../types/types";

export const useSchemaSettings = async (keys: Set<string>) => {
  const route = useRoute();
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
    });

    if (response) {
      const settingsArray: { key: string; value: string }[] = Array.isArray(
        response.data
      )
        ? response.data[0]._settings
        : response.data._settings;

      const settingsMap: Record<string, string> = {};
      settingsArray.forEach((item) => {
        settingsMap[item.key] = item.value;
      });

      return settingsMap;
    }
  }
};
