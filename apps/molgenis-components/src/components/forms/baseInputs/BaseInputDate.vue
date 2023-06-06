<template>
  <input
    v-if="readonly"
    :id="id"
    readonly
    v-model="modelValue"
    class="form-control"
    :class="{ 'is-invalid': errorMessage }"
    :required="required" />
  <FlatPickr
    v-else
    :id="id"
    :modelValue="modelValue"
    style="background: white"
    class="form-control active"
    :config="config"
    :placeholder="placeholder"
    :disabled="readonly"
    @input="$emit('update:modelValue', $event.target.value)" />
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FlatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.css";

export default {
  extends: BaseInput,
  components: {
    FlatPickr,
  },
  props: {
    readonly: { type: Boolean, default: false },
    config: {
      type: Object,
      required: true,
    },
  },
};
</script>
