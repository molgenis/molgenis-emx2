export default {
  PAGE_SIZE_OPTIONS: [10, 20, 50, 100],
  PAGE_SIZE_DEFAULT: 20,
};

export const DATA_NOT_FOUND_ERROR =
  "Might you need to sign in or ask permission?";

// Triggers and delays for the VMenu navigation menus (Navigation.vue, NavigationMenuItems.vue).
// "focus" is deliberately left out: it closes the menu on blur when floating-vue
// moves focus into the opened popper, which breaks click-to-toggle.
export const NAVIGATION_MENU_TRIGGERS = ["hover", "click"];
export const NAVIGATION_MENU_DELAY = { show: 0, hide: 0 };
