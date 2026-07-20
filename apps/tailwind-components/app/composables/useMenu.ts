import { assertMenu } from "../utils/typeUtils";
import type { Menu } from "../../types/types";
import { useSchemaSettings } from "./useSchemaSettings";
import { useRoute } from "#app";
import { computed, ref, watch } from "vue";

const MENU_SETTING_KEY = "tw-menu";

function parseMenuItems(menuJson: string): Menu {
  try {
    const menu = JSON.parse(menuJson);
    assertMenu(menu);
    return menu;
  } catch (error) {
    console.error("Error parsing menu JSON", error);
    throw error;
  }
}

async function fetchMenu(schema: string | undefined) {
  if (schema) {
    // Fetch menu settings for the current schema
    const schemaSettings = await useSchemaSettings(new Set([MENU_SETTING_KEY]));
    const menuSetting = schemaSettings?.value?.[MENU_SETTING_KEY];
    return typeof menuSetting === "string" ? parseMenuItems(menuSetting) : [];
  } else {
    // Fetch system menu
    return [];
  }
}

export const useMenu = async () => {
  const route = useRoute();
  const menu = ref<Menu>([]);

  watch(
    () => route.params.schema,
    async (newSchema) => {
      if (newSchema) {
        const resolvedSchema = Array.isArray(newSchema)
          ? newSchema[0]
          : newSchema;
        const newMenu = await fetchMenu(resolvedSchema);
        menu.value = newMenu;
      } else {
        menu.value = [];
      }
    },
    { immediate: true }
  );

  return menu;
};
