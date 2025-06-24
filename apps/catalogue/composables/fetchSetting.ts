import { useRuntimeConfig } from "#app";

export const fetchSetting = (settingKey: string) => {
  let body = {
    query: `{_settings (keys: ["${settingKey}"]){ key, value }}`,
  };

  const config = useRuntimeConfig();
  const schema = config.public.schema;

  return $fetch(`/${schema}/graphql`, {
    method: "POST",
    body,
  });
};
