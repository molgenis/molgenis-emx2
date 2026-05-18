import { useRuntimeConfig } from "#app";
import { fetchSettings } from "#imports";
import type { ISetting } from "../../../metadata-utils/src";

export const fetchSetting = (
  settingKey: string
): Promise<{ data: { _settings: ISetting[] } }> => {
  return fetchSettings([settingKey]);
};
