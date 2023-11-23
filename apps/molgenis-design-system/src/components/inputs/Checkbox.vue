<template>
  <div class="flex items-start gap-1">
    <input :id="id" type="checkbox" :name="name" :value="value" class="mt-1 mr-1 accent-blue-700"/>
    <label :for="id" class="" :data-description="description !== undefined">
      <span class="block data-[description='true']:font-bold">{{ label }}</span>
      <span v-if="description" class="block leading-5 text-gray-600">{{ description }}</span>
    </label>
  </div>
</template>

<script setup lang="ts">
interface Props {
  id: string,
  label: string,
  name?: string,
  value?: string
  description?: string,
}

defineProps<Props>()

</script>