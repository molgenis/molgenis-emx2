<script setup lang="ts">
withDefaults(
  defineProps<{
    item: { label: string; value: string };
    isNonCommercial?: boolean;
  }>(),
  {
    isNonCommercial: false,
  }
);

const emit = defineEmits(["removeItemFromCart"]);
</script>

<template>
  <div class="card-body d-flex border-bottom">
    <div>
      <span
        v-if="isNonCommercial"
        class="fa-brands fa-creative-commons-nc-eu text-danger non-commercial mr-2"
        title="Not available for commercial use"
      ></span>

      <span> {{ item.label }}</span>
    </div>
    <div class="pl-3 ml-auto" @click="$emit('removeItemFromCart', item.value)">
      <span class="fa fa-times text-bold remove-item" title="Remove"></span>
    </div>
  </div>
</template>

<style scoped>
.remove-item:hover {
  cursor: pointer;
}
</style>
