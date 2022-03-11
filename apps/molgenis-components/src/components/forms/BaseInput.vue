<template>
  <div class="form-group">
    <label v-if="label !== null && label !== undefined" :id="id">{{ label }}</label>
    <input
      type="email"
      class="form-control"
      :id="id"
      :aria-describedby="id"
      :placeholder="label"
    />
    <small v-if="isNonEmptyString(helpText)" :id="id + '-help-text'" class="form-text text-muted"
      >{{ helpText }}</small
    >
  </div>
</template>

<script>
export default {
  name: "BaseInput",
  props: {
    id: {
      type: String,
      required: true
    },
    label: {
      type: String,
      required: false
    },
    helpText: {
      type: String,
      required: false
    },
    type: {
      
    }
  },
  methods: {
    isNonEmptyString(s) {
      return s !== null && s !== undefined && s.length
    }
  }
};
</script>