import type { ISetting } from "../../../metadata-utils/src/types";

export const CATALOGUE_ENABLE_PAPER_EXTRACTING =
  "CATALOGUE_ENABLE_PAPER_EXTRACTING";
export const CATALOGUE_PAPER_EXTRACTING_SCHEMA =
  "CATALOGUE_PAPER_EXTRACTING_SCHEMA";

export const PAPER_EXTRACTING_SETTING_KEYS = [
  CATALOGUE_ENABLE_PAPER_EXTRACTING,
  CATALOGUE_PAPER_EXTRACTING_SCHEMA,
];

export interface PaperExtractingSettings {
  enabled: boolean;
  schema: string;
}

/**
 * Paper extracting is only enabled when CATALOGUE_ENABLE_PAPER_EXTRACTING is
 * 'true' and CATALOGUE_PAPER_EXTRACTING_SCHEMA names the schema with the jobs.
 */
export function parsePaperExtractingSettings(
  settings: ISetting[] | undefined
): PaperExtractingSettings {
  const findValue = (key: string) =>
    settings?.find((setting) => setting.key === key)?.value?.trim() ?? "";

  const schema = findValue(CATALOGUE_PAPER_EXTRACTING_SCHEMA);
  const enabled =
    findValue(CATALOGUE_ENABLE_PAPER_EXTRACTING).toLowerCase() === "true" &&
    schema !== "";

  return { enabled, schema };
}
