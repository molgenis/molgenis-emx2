import { assertMenu } from "../utils/typeUtils";
import type { Menu } from "../../types/types";
import { useSchemaSettings } from "./useSchemaSettings";
import { ref, useRoute, watch } from "#imports";

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

async function fetchMenu() {
  const schemaSettings = await useSchemaSettings(new Set(["menu"]));
  return schemaSettings?.menu ? parseMenuItems(schemaSettings.menu) : [];
}

export const useMenu = async () => {
  const menu = ref<Menu>([]);
  const route = useRoute();

  watch(
    () => route.params.schema,
    async (newSchema) => {
      if (newSchema) {
        const newMenu = await fetchMenu();
        menu.value = newMenu;
      } else {
        menu.value = [];
      }
    }
  );

  menu.value = await fetchMenu();
  return menu;
};
