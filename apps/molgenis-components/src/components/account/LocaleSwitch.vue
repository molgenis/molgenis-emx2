<template>
  <div class="d-inline-flex from-group">
    <select
      class="form-control bg-primary text-white border-0 mr-2 pl-0 pr-0"
      :modelValue="modelValue"
      @input="emit"
    >
      <option v-for="locale in locales" :key="locale">{{ locale }}</option>
    </select>
  </div>
</template>
<script>
export default {
  name: "LocaleSwitch",
  props: {
    modelValue: {
      type: String,
      required: true,
    },
    locales: {
      type: Array,
      required: true,
    },
  },
  methods: {
    emit(event) {
      if (event.target.value !== undefined) {
        this.$emit("update:modelValue", event.target.value);
      } else {
        this.$emit("update:modelValue", "en");
      }
    },
  },
};
</script>
