<template>
  <label class="m-0">
    <input
      :id="uniqueSelector"
      type="checkbox"
      v-model="checkboxModel"
      :value="option"
      class="mr-1"
    />
    <span>{{ option.text }}</span>
  </label>
</template>

<script>
export default {
  props: {
    option: {
      type: Object,
      required: true,
    },
    /** the array with the selected values or the option if selected */
    modelValue: {
      type: [Array, Object],
      required: false,
    },
  },
  computed: {
    checkboxModel: {
      get() {
        return this.modelValue;
      },
      set(newValue) {
        this.$emit("update:modelValue", newValue);
      },
    },
  },
  data() {
    return {
      uniqueSelector: new Date().getMilliseconds() + Math.random(),
    };
  },
};
</script>
<style scoped>
input[type="checkbox"] {
  accent-color: var(--primary);
}

input[type="checkbox"]:hover {
  cursor: pointer;
}
</style>
