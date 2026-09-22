import { useSettings } from "./useSettings";
import { useSchemaSettings } from "./useSchemaSettings";
import type { Settings } from "../../types/types";

const LOGO_URL_SETTING = "logoURL";

function toLogoUrl(settings?: Settings): string | undefined {
  const value = settings?.[LOGO_URL_SETTING];
  return typeof value === "string" ? value : undefined;
}

export async function useLogo(
  schema?: string | string[]
): Promise<string | undefined> {
  let logoUrl: string | undefined = undefined;

  const systemSettings = await useSettings(new Set([LOGO_URL_SETTING]));
  const systemLogoUrl = toLogoUrl(systemSettings.value);
  if (systemLogoUrl) {
    logoUrl = systemLogoUrl;
  }

  if (schema) {
    const schemaSettings = await useSchemaSettings(new Set([LOGO_URL_SETTING]));
    const schemaLogoUrl = toLogoUrl(schemaSettings?.value);
    if (schemaLogoUrl) {
      logoUrl = schemaLogoUrl;
    }
  }

  return logoUrl;
}
