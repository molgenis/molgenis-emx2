export const fetchSetting = (settingKey: string) => {
  let body = {
    query: `{_settings (keys: ["${settingKey}"]){ key, value }}`,
  };

  const route = useRoute();
  const config = useRuntimeConfig();
  return $fetch(`/${route.params.schema}/catalogue/graphql`, {
    method: "POST",
    baseURL: config.public.apiBase,
    body,
  });
};
