<template>
  <input
    v-if="readonly"
    :id="id"
    readonly
    v-model="value"
    class="form-control"
  />
  <FlatPickr
    v-else
    :id="id"
    :value="value"
    style="background: white"
    class="form-control active"
    :config="config"
    :placeholder="placeholder"
    :disabled="readonly"
    @input="$emit('input', $event)"
  />
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
  },
  computed: {
    config() {
      return {
        wrap: false,
        dateFormat: "Y-m-d",
        allowInput: false,
        clickOpens: !this.readonly,
      };
    },
  },
};
</script>