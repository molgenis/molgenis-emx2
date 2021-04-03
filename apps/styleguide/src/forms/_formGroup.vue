/** internal component that will not be shown in style guide */

<template>
  <div class="form-group" :class="inplace ? 'm0' : ''">
    <label v-if="label && !inplace" :for="id">
      <b>{{ label }}</b>
    </label>
    <span v-if="required" class="float-right">(required)</span>
    <slot />
    <small
      v-if="description && !inplace"
      :id="id + 'Description'"
      class="form-text text-muted"
    >
      {{ description }}
    </small>
    <div v-if="errorMessage" class="text-danger">{{ errorMessage }}</div>
  </div>
</template>

<script>
export default {
  props: {
    /** id for which this is the group */
    id: String,
    /** value to be shown as input */
    placeholder: String,
    /** label to be shown next to the input */
    label: String,
    /** optional description string shown below */
    description: String,
    /** if required */
    required: Boolean,
    /** String with graphqlError state */
    errorMessage: String,
    /** whether inplace */
    inplace: Boolean,
  },
};
</script>
