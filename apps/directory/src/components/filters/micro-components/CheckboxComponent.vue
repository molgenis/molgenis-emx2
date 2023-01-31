<template>
  <input
    :id="uniqueSelector"
    type="checkbox"
    :name="uniqueSelector"
    v-model="checkboxModel"
    :value="option"
  /><label :for="uniqueSelector"
    ><span>{{ option.text }}</span></label
  >
  <!-- custom control class seems to be broken  -->
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
