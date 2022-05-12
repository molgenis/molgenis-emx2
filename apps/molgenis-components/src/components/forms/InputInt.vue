<template>
  <FormGroup :id="id" :label="label" :description="description">
    <BaseIntInput
      :id="id"
      :value="value"
      :placeholder="placeholder"
      @input="$emit('input', $event)"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import BaseIntInput from "./baseInputs/BaseInputInt.vue"
import FormGroup from "./FormGroup.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup, BaseIntInput
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
