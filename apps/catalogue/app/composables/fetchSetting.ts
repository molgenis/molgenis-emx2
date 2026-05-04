import { useRuntimeConfig } from "#app";
import type { ISetting } from "../../../metadata-utils/src";

export const fetchSetting = (
  settingKey: string
): Promise<{ data: { _settings: ISetting[] } }> => {
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
