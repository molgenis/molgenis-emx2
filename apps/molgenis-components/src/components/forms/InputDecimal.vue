<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <BaseInputDecimal
      :id="id"
      :value="value"
      :class="{ 'is-invalid': errorMessage }"
      :placeholder="placeholder"
      :required="required"
      @input="$emit('input', $event)"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import BaseInputDecimal from "./baseInputs/BaseInputDecimal.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    BaseInputDecimal,
  },
};
</script>

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
