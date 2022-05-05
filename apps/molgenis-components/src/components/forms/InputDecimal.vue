<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    :errorMessage="validDecimal"
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
  computed: {
    validDecimal() {
      console.log(this.value);
      console.log(parseFloat(this.value));
      return !isNaN(parseFloat(this.value))
        ? undefined
        : "Invalid decimal value";
    },
  },
  methods: {
    emitIfValid(event) {
      const value = parseFloat(event.target.value);
      if (event.target.value === "") {
        this.$emit("input", null);
      }
      if (!isNaN(value)) {
        this.$emit("input", event.target.value);
      }
    },
    handleKeyValidity(event) {
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
  },
};
</script>

<style scoped>
.is-invalid {
  background-image: none;
  padding-right: 0.75rem;
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
