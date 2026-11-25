import { assertMenu } from "../utils/typeUtils";
import type { Menu } from "../../types/types";
import { useSchemaSettings } from "./useSchemaSettings";
import { useRoute } from "#app";
import { ref, watch } from "vue";

const MENU_SETTING_KEY = "tw-menu";
const MOBILE_MENU_SETTING_KEY = "tw-mobile-menu";
export const MAX_MOBILE_MENU_ITEMS = 3;

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

/**
 * The mobile menu is optional, so an invalid setting hides the mobile menu row
 * instead of breaking the page: it must be a valid menu with at most
 * MAX_MOBILE_MENU_ITEMS items and no submenus.
 */
export function parseMobileMenuItems(menuSetting: unknown): Menu {
  if (typeof menuSetting !== "string") {
    return [];
  }
  try {
    const menu = JSON.parse(menuSetting);
    assertMenu(menu);
    const hasSubmenu = menu.some((item) => (item.submenu?.length ?? 0) > 0);
    return menu.length <= MAX_MOBILE_MENU_ITEMS && !hasSubmenu ? menu : [];
  } catch {
    return [];
  }
}

async function fetchMenus(): Promise<{ menu: Menu; mobileMenu: Menu }> {
  const schemaSettings = await useSchemaSettings(
    new Set([MENU_SETTING_KEY, MOBILE_MENU_SETTING_KEY])
  );
  const menuSetting = schemaSettings?.value?.[MENU_SETTING_KEY];
  return {
    menu: typeof menuSetting === "string" ? parseMenuItems(menuSetting) : [],
    mobileMenu: parseMobileMenuItems(
      schemaSettings?.value?.[MOBILE_MENU_SETTING_KEY]
    ),
  };
}

export const useMenu = async () => {
  const route = useRoute();
  const menu = ref<Menu>([]);
  const mobileMenu = ref<Menu>([]);

  watch(
    () => route.params.schema,
    async (newSchema) => {
      if (newSchema) {
        const menus = await fetchMenus();
        menu.value = menus.menu;
        mobileMenu.value = menus.mobileMenu;
      } else {
        menu.value = [];
        mobileMenu.value = [];
      }
    },
    { immediate: true }
  );

  return { menu, mobileMenu };
};
