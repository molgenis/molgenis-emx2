<template>
  <div class="flex items-start gap-1">
    <input
      :id="id"
      type="checkbox"
      :name="name"
      :value="value"
      class="mt-1 mr-1 accent-blue-700"
      :checked="checked"
    />
    <label :for="id" :data-description="description !== undefined">
      <span class="block data-[description='true']:font-bold">
        {{ label }}
      </span>
      <slot name="description"></slot>
    </label>
  </div>
</template>

<script setup lang="ts">
interface Props {
  id: string;
  label: string;
  name?: string;
  value?: string;
  description?: string;
  checked?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  checked: false,
});
</script>
