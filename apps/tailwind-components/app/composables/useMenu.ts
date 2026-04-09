import { assertMenu } from "../utils/typeUtils";
import type { Menu } from "../../types/types";
import { useSchemaSettings } from "./useSchemaSettings";
import { computed, ref, useRoute, watch } from "#imports";

const route = useRoute();
const schema = computed(() =>
  Array.isArray(route.params.schema)
    ? route.params.schema[0]
    : route.params.schema
);

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
  if (schema.value) {
    // Fetch menu settings for the current schema
    const schemaSettings = await useSchemaSettings(new Set(["menu"]));
    return schemaSettings?.menu ? parseMenuItems(schemaSettings.menu) : [];
  } else {
    // Fetch system menu
    return [];
  }
}

export const useMenu = async () => {
  console.log("Initializing menu...");
  const menu = ref<Menu>([]);
  const route = useRoute();

  watch(
    () => route.params.schema,
    async (newSchema) => {
      if (newSchema) {
        const newMenu = await fetchMenu();
        menu.value = newMenu;
        console.log("Menu updated based on schema change:", menu.value);
      } else {
        menu.value = [];
        console.log("No schema found, use top menu ?");
      }
    }
  );

  menu.value = await fetchMenu();
  return menu;
};
