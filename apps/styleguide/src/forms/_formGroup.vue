/** internal component that will not be shown in style guide */

<template>
  <div class="form-group">
    <label v-if="label && !inplace" :for="id">
      <b>{{ label }}</b>
    </label>
    <span v-if="nullable" class="float-right">(optional)</span>
    <slot />
    <small
      v-if="help && !inplace"
      :id="id + 'Help'"
      class="form-text text-muted"
    >
      {{ help }}
    </small>
    <div v-if="error" class="text-danger">{{ error }}</div>
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
    /** optional help string shown below */
    help: String,
    /** if optional */
    nullable: Boolean,
    /** String with error state */
    error: String,
    /** whether inplace */
    inplace: Boolean,
  },
};
</script>
