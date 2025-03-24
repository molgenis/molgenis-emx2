<template>
  <ul
    role="listbox"
    ref="ul"
    :aria-expanded="isExpanded"
    class="absolute b-0 w-full overflow-y-scroll z-10 bg-input border"
    :class="{
      hidden: !isExpanded,
      'h-44': isExpanded && hasFixedHeight,
      'shadow-inner': isExpanded,
    }"
    :tabindex="isExpanded ? 1 : 0"
  >
    <slot></slot>
  </ul>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    isExpanded: boolean;
    hasFixedHeight: boolean;
  }>(),
  {
    isExpanded: false,
    hasFixedHeight: false,
  }
);

const ul = useTemplateRef<HTMLUListElement>("ul");
defineExpose({
  ul,
});
</script>
