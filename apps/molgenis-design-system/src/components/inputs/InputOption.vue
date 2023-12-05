<template>
  <div class="flex items-start gap-1">
    <input
      :id="id"
      :type="type"
      :name="name"
      :value="value"
      class="peer mt-1 mr-1 ml-1 accent-blue-700"
      :checked="checked"
      :required="required"
    />
    <label
      :for="id"
      class="
        peer-required:[&>span]:after:content-['*'] 
        peer-required:[&>span]:after:text-[0.9em]
      peer-required:[&>span]:after:text-red-500
        peer-required:[&>span]:after:font-bold
        peer-required:[&>span]:after:ml-1
      "
      >
      <span class="block">
        {{ label }}
      </span>
      <slot name="description"></slot>
    </label>
  </div>
</template>

<script setup lang="ts">
interface Props {
  id: string;
  type: 'checkbox' | 'radio',
  label: string;
  name?: string;
  value?: string;
  description?: string;
  checked?: boolean;
  required?: boolean,
}

withDefaults(
  defineProps<Props>(),
  {
    type: 'checkbox'
  }
);
</script>
