<template>
  <FormGroup :id="id" :label="label" :description="description">
    <input
      :id="id"
      type="number"
      step="1"
      :value="value"
      class="form-control"
      :aria-describedby="id + 'Help'"
      :placeholder="placeholder"
      @keypress="handleKeyValidity"
      @input="emitIfValid"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import { isNumericKey } from "./utils/InputUtils";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
  },
  methods: {
    emitIfValid(event) {
      const value = parseInt(event.target.value);
      if (event.target.value === "") {
        this.$emit("input", null);
      }
      if (!isNaN(value)) {
        this.$emit("input", value);
      }
    },
    handleKeyValidity(event) {
      if (!isNumericKey(event)) event.preventDefault();
    },
  },
};
</script>

<docs>
  <template>
    <demo-item>
      <InputInt
        id="input-int"
        v-model="value"
        label="My int input label"
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
