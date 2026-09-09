<script setup lang="ts">
import CustomTooltip from "../CustomTooltip.vue";
import BaseIcon from "../BaseIcon.vue";

withDefaults(
  defineProps<{
    name: string;
    definition?: string | null;
    hasChildren?: boolean;
    collapsed?: boolean;
    marker?: "flush" | "blank" | "bullet" | "connector";
  }>(),
  {
    hasChildren: false,
    collapsed: false,
    marker: "blank",
  }
);

defineEmits<{ (e: "toggle"): void }>();
</script>

<template>
  <div class="flex items-center">
    <span
      v-if="hasChildren || marker !== 'flush'"
      class="marker-gutter shrink-0"
    >
      <button
        v-if="hasChildren"
        type="button"
        data-marker="caret"
        class="text-link rounded-full hover:bg-link-hover hover:cursor-pointer p-0.5"
        :class="{ 'rotate-180': collapsed }"
        :aria-expanded="!collapsed"
        :aria-label="(collapsed ? 'Expand ' : 'Collapse ') + name"
        @click="$emit('toggle')"
      >
        <BaseIcon name="caret-up" :width="20" />
      </button>
      <span
        v-else-if="marker === 'bullet'"
        data-marker="bullet"
        class="w-1.5 h-1.5 rounded-full bg-current"
      />
      <BaseIcon
        v-else-if="marker === 'connector'"
        data-marker="connector"
        name="tree-connector"
        :width="20"
        class="text-disabled"
      />
    </span>
    <span
      class="flex justify-center items-start"
      :class="{ 'cursor-pointer hover:underline': hasChildren }"
      @click="hasChildren && $emit('toggle')"
    >
      {{ name }}
    </span>
    <div class="inline-flex items-center whitespace-nowrap">
      <div v-if="definition" class="inline-block ml-1">
        <CustomTooltip
          label="Read more"
          hoverColor="white"
          :content="definition"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.marker-gutter {
  position: relative;
  width: 1.25rem;
  height: 1.25rem;
  flex: none;
}
.marker-gutter > * {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
}
</style>
