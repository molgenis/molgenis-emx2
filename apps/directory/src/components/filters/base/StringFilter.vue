<template>
  <div class="input-group mb-3">
    <input
      :name="name"
      :disabled="disabled"
      type="text"
      class="form-control"
      v-model="model"
      :placeholder="placeholder"
      :aria-label="placeholder"
    />
    <button
      :disabled="disabled"
      class="btn btn-outline-secondary"
      type="button"
      @click.prevent="model = ''"
    >
      <span class="fa-solid fa-xmark"></span>
    </button>
  </div>
</template>

<script>
export default {
  name: "StringFilter",
  props: {
    disabled: {
      type: Boolean,
      required: false,
      default: () => false,
    },
    /**
     * The HTML input element name.
     */
    name: {
      type: String,
      required: true,
    },
    /**
     * The HTML input element placeholder.
     */
    placeholder: {
      type: String,
      required: false,
      default: () => "",
    },
    modelValue: {
      type: String,
      default: () => "",
    },
  },
  computed: {
    model: {
      get() {
        return this.modelValue || "";
      },
      set(value) {
        this.$emit("update:modelValue", value === "" ? undefined : value);
      },
    },
  },
};
</script>
