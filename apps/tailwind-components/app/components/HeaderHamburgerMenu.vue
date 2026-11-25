<script setup lang="ts">
import BaseIcon from "./BaseIcon.vue";
import HeaderHamburgerMenuItems from "./HeaderHamburgerMenuItems.vue";
import SideModal from "./SideModal.vue";
import type { MenuItem } from "../../types/types";

defineProps<{
  navigation: MenuItem[];
}>();

// links inside the ui app navigate without a page load, so the menu would stay open over the new page
function closeOnLinkClick(event: MouseEvent, hide: () => void) {
  if (event.target instanceof Element && event.target.closest("a")) {
    hide();
  }
}
</script>

<template>
  <SideModal
    :slideInRight="false"
    :fullScreen="true"
    :includeFooter="false"
    type="light"
  >
    <template #default="{ hide }">
      <nav
        aria-label="Main menu"
        class="min-h-[calc(100vh-4rem)] bg-form px-6 pb-24 pt-19"
        @click="closeOnLinkClick($event, hide)"
      >
        <HeaderHamburgerMenuItems :items="navigation" />
      </nav>
    </template>

    <template #button>
      <button
        type="button"
        class="flex items-center text-menu"
        aria-label="Open menu"
      >
        <BaseIcon name="menu" />
      </button>
    </template>
  </SideModal>
</template>
