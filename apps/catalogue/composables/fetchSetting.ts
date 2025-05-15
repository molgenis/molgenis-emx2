import { useRoute } from "#app/composables/router";

export const fetchSetting = (settingKey: string) => {
  let body = {
    query: `{_settings (keys: ["${settingKey}"]){ key, value }}`,
  };

  const route = useRoute();
  return $fetch(`/graphql`, {
    method: "POST",
    body,
  });
};
