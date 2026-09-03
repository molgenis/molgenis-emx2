<script setup lang="ts">
import CustomTooltip from "../CustomTooltip.vue";
import BaseIcon from "../BaseIcon.vue";

withDefaults(
  defineProps<{
    name: string;
    definition?: string | null;
    hasChildren?: boolean;
    collapsed?: boolean;
    /** "bullet": a flat-list row. "connector": a non-root tree leaf. "blank":
     *  a tree root leaf — no icon, but the gutter stays so its text lines up
     *  with sibling rows that DO carry a caret. "flush": the standalone
     *  single value, which has no siblings to line up with, so no gutter
     *  at all and the text sits flush left. */
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
    <!-- Fixed size, and every marker is positioned, not flowed: the tallest
         marker (the caret's 24x24 hit target) must never set the row's
         height, so none of them may occupy flow space here. Omitted only for
         "flush", which has no sibling row to keep text aligned with. -->
    <span
      v-if="hasChildren || marker !== 'flush'"
      class="relative w-5 h-5 shrink-0"
    >
      <button
        v-if="hasChildren"
        type="button"
        data-marker="caret"
        class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 text-link rounded-full hover:bg-link-hover hover:cursor-pointer p-0.5"
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
        class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-1.5 h-1.5 rounded-full bg-current"
      />
      <!-- collapsible-list-item is a 20x22 asset whose dashed arm sits at
           y=21, not at its own y=11 centre; the extra 10px in the y offset
           lands the arm, not the box, on the gutter's centre point. -->
      <BaseIcon
        v-else-if="marker === 'connector'"
        data-marker="connector"
        name="collapsible-list-item"
        :width="20"
        class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-[calc(50%+10px)] text-disabled"
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
        <!-- The row's own colours are a CSS-cascade concern now
             (.surface-inverted on an ancestor); hoverColor only ever chose
             between two fixed values here, never one derived from a
             surface, so it stays the content-surface one. -->
        <CustomTooltip
          label="Read more"
          hoverColor="white"
          :content="definition"
        />
      </div>
    </div>
  </div>
</template>
