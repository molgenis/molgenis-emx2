<template>
  <label :for="id" class="input-label">
    <span>{{ label }}</span>
    <span class="input-description">{{ description }}</span>
  </label>
</template>

<script>
// @displayName InputLabel
// Create a label and an optional description for an input element
export default {
  props: {
    // the identifier of the input element to be passed on to the `for` attribute
    id: {
      type: String,
      required: true,
    },
    // a label that describes the input element
    label: {
      type: String,
      required: true,
    },
    // an optional description that provides context for the input element (e.g., examples, etc.)
    description: {
      type: String,
      required: false,
    },
  },
};
</script>

<style lang="scss">
.input-label {
  display: block;
  color: $gray-900;
  font-size: 13pt;

  .input-description {
    display: block;
    margin-top: 6px;
    font-size: 11pt;
    color: $gray-400;
  }
}
</style>
