<template>
  <label 
    :for="id"
    :data-valid="valid"
    :data-error="error"
    :data-required="required"
    :data-is-heading="label_as_heading"
    class="
      block
      mb-1
    
      data-[is-heading='true']:text-heading-base
      data-[is-heading='true']:font-semibold
    
      data-[required='true']:after:content-['*']
      data-[required='true']:after:text-[0.9em]
      data-[required='true']:after:text-red-500
      data-[required='true']:after:font-bold
      data-[required='true']:after:ml-1
    
    data-[valid='true']:text-green-800
    data-[error='true']:text-red-500
    "
  >
    {{ label }}
  </label>
  <p
    v-if="description"
    :id="`input-description-${id}`"
    class="mb-1"
    v-html="description"
  />
</template>

<script setup lang="ts">
interface Props {
  id: string,
  label: string,
  description?: string,
  
  disabled?: boolean;
  required?: boolean;
  valid?: boolean,
  error?: boolean;
  label_as_heading?: boolean,
}

defineProps<Props>();

</script>