<template>
  <input
    type="checkbox"
    :id="id"
    @change="emit('change', ($event.target as HTMLInputElement).checked)"
    :checked="isChecked"
    hidden
  />
  <label class="btn" :for="id">
    <span v-show="!isChecked"><i class="fa-regular fa-lg fa-square"></i></span>
    <span v-show="isChecked"
      ><i class="fa-regular fa-lg fa-check-square"></i
    ></span>
  </label>
</template>

<script setup lang="ts">
defineProps<{
  id: string;
  isChecked?: boolean;
}>();

const emit = defineEmits(["change"]);
</script>
