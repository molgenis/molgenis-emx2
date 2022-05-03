<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    v-on="$listeners"
  >
    <input
      :id="id"
      type="number"
      step="1"
      :value="value"
      :class="{ 'form-control': true, 'is-invalid': errorMessage }"
      :aria-describedby="id + 'Help'"
      :placeholder="placeholder"
      @keypress="keyhandler"
      @input="$emit('input', $event.target.value)"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import constants from "../constants.js";

const { CODE_0, CODE_9, CODE_BACKSPACE, CODE_DELETE } = constants;

export default {
  extends: BaseInput,
  components: {
    FormGroup,
  },
  props: {
    parser: {
      default() {
        return parseFloat;
      },
    },
    errorMessage: { type: String, default: null },
  },
  methods: {
    keyhandler(event) {
      if (!this.isValidKey(event)) event.preventDefault();
    },
    isValidKey(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      return (
        (keyCode >= CODE_0 && keyCode <= CODE_9) ||
        keyCode === CODE_BACKSPACE ||
        keyCode === CODE_DELETE
      );
    },
  },
};
</script>

<style scoped>
.is-invalid {
  background-image: none;
  padding-right: 0.75rem;
}

span:hover .hoverIcon {
  visibility: visible;
}
</style>

<docs>
  <template>
    <demo-item id="input-decimal-demo" label="Input decimal">
      <InputDecimal
        id="input-decimal"
        v-model="value"
        label="My decimal input label"
        description="Some help needed?"
      />
      You typed: {{ JSON.stringify(value) }}
    </demo-item>
  </template>
  <script>
  export default {
    data: function () {
      return {
        value: null,
      };
    },
  };
  </script>
</docs>
