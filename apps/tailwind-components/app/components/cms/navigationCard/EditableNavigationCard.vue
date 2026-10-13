<script setup lang="ts">
import { ref } from "vue";

import NavigationCard from "./NavigationCard.vue";
import ComponentActions from "../ComponentActions.vue";

import type { INavigationCards } from "../../../../types/cms";

withDefaults(defineProps<INavigationCards & { isEditable?: boolean }>(), {
  isEditable: false,
});

const emit = defineEmits(["edit", "delete", "move"]);
const showMenu = ref<boolean>(false);
</script>

<template>
  <VMenu
    v-if="isEditable"
    v-model:show="showMenu"
    :popperTriggers="['hover', 'focus']"
    :delay="{ show: 100, hide: 50 }"
    placement="bottom-start"
    noAutoFocus
  >
    <template #popper>
      <ComponentActions
        name="NavigationCard"
        :id="`${id}-toolbar`"
        :aria-controls="id"
        @edit="$emit('edit')"
        @delete="$emit('delete')"
        @move="$emit('move', $event)"
      />
    </template>
    <NavigationCard
      :id="id"
      :title="title"
      :description="description"
      :url="url"
      :urlLabel="urlLabel"
      :urlIsExternal="urlIsExternal"
    />
  </VMenu>
  <NavigationCard
    v-else
    :id="id"
    :title="title"
    :description="description"
    :url="url"
    :urlLabel="urlLabel"
    :urlIsExternal="urlIsExternal"
  />
</template>
