export const fetchSetting = (settingKey: string) => {
  let body = {
    query: `{_settings (keys: ["${settingKey}"]){ key, value }}`,
  };

  const route = useRoute();
  return $fetch(`/${route.params.schema}/graphql`, {
    method: "POST",
    body,
  });
};
