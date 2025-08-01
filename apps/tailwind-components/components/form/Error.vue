<template>
  <div class="flex flex-row bg-invalid p-2 justify-between min-h-[64px]">
    <div></div>
    <div class="flex flex-row gap-1">
      <BaseIcon name="info" class="text-invalid stroke-2 min-h-6 min-w-6" />
      <span class="my-auto text-invalid font-bold px-1">{{ message }}</span>
    </div>

    <ButtonBar v-if="showPrevNextButtons">
      <Button
        class="border-gray-200"
        :icon-only="true"
        type="tertiary"
        icon="arrow-up"
        label="go to previous error"
        @click="$emit('error-prev')"
      />
      <Button
        class="border-gray-200"
        :icon-only="true"
        type="tertiary"
        icon="arrow-down"
        label="go to next error"
        @click="$emit('error-next')"
      />
    </ButtonBar>
    <div v-else></div>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    message: string;
    showPrevNextButtons?: boolean;
  }>(),
  {
    showPrevNextButtons: true,
  }
);

defineEmits(["error-prev", "error-next"]);
</script>
