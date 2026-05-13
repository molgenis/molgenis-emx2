import { useRuntimeConfig } from "#app";
import type { ISetting } from "../../../metadata-utils/src";

export const fetchSettings = (
  settingKeys: string[]
): Promise<{ data: { _settings: ISetting[] } }> => {
  const body = {
    query: `{_settings (keys: ${JSON.stringify(settingKeys)}){ key, value }}`,
  };

  const config = useRuntimeConfig();
  const schema = config.public.schema;

  return $fetch(`/${schema}/graphql`, {
    method: "POST",
    body,
  });
};
