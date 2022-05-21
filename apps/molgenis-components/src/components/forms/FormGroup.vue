<template>
  <div class="form-group">
    <label v-if="label !== null && label !== undefined" :for="id">
      <b> {{ label }}</b>
    </label>
    <small
      v-if="hasDescription(description)"
      :id="id + '-help-text'"
      class="form-text text-muted"
    >
      {{ description }}
    </small>
    <slot></slot>
    <small v-if="errorMessage" class="text-danger form-text">
      {{ errorMessage }}
    </small>
  </div>
</template>

<script>
export default {
  name: "FormGroup",
  props: {
    id: {
      type: String,
      required: true,
    },
    label: {
      type: String,
      required: false,
    },
    description: {
      type: String,
      required: false,
    },
    errorMessage: {
      type: String,
      required: false,
    },
  },
  methods: {
    hasDescription(description) {
      return (
        description !== null && description !== undefined && description.length
      );
    },
  },
};
</script>