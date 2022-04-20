<template>
  <div class="form-group">
    <label v-if="label !== null && label !== undefined" :for="id">{{
      label
    }}</label>
    <slot></slot>
    <small
      v-if="isNonEmptyString(description)"
      :id="id + '-help-text'"
      class="form-text text-muted"
      >{{ description }}</small
    >
  </div>
</template>

<script>
export default {
  name: 'FormGroup',
  props: {
    id: {
      type: String,
      required: true
    },
    label: {
      type: String,
      required: false
    },
    description: {
      type: String,
      required: false
    },
  },
  methods: {
    isNonEmptyString(s) {
      return s !== null && s !== undefined && s.length;
    }
  }
};
</script>