<template>
  <div class="flex flex-row bg-invalid p-2 justify-between">
    <div></div>
    <div class="flex flex-row gap-1">
      <BaseIcon name="info" class="text-invalid stroke-2 min-h-6 min-w-6" />
      <span class="my-auto text-invalid font-bold px-1">{{ message }}</span>
    </div>

    <ButtonBar>
      <Button
        :icon-only="true"
        type="tertiary"
        icon="caret-up"
        label="Go to previous error"
        @click="$emit('error-prev')"
      />
      <Button
        :icon-only="true"
        type="tertiary"
        icon="caret-down"
        label="Go to next error"
        @click="$emit('error-next')"
      />
    </ButtonBar>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message: string;
}>();

defineEmits(["error-prev", "error-next"]);
</script>
