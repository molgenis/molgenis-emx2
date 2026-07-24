<script setup lang="ts">
import Button from "../Button.vue";

const props = withDefaults(defineProps<{ name?: string }>(), {});
</script>
<template>
  <div
    class="z-40 absolute -translate-y-full left-0 top-0 inline-flex flex-row items-center gap-2 p-1 divide-x group border border-theme rounded-theme [background-image:var(--background-color-bulk-gradient)] shadow-lg"
  >
    <div class="flex flex-row items-center gap-2">
      <Button
        icon-only
        size="tiny"
        icon="edit"
        type="inline"
        :label="`Edit ${name}`"
        @click="$emit('edit')"
      />
      <Button
        icon-only
        size="tiny"
        icon="trash"
        type="inline"
        :label="`Delete ${name}`"
        @click="$emit('delete')"
      />
    </div>
  </div>
</template>
