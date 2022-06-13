<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage"
  >
    <BaseIntInput
      :id="id"
      :value="value"
      :placeholder="placeholder"
      :readonly="readonly"
      :required="required"
      :class="{ 'is-invalid': errorMessage }"
      @input="$emit('input', $event)"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import BaseIntInput from "./baseInputs/BaseInputInt.vue";
import FormGroup from "./FormGroup.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    BaseIntInput,
  },
};
</script>

<docs>
  <template>
  <div>
    <demo-item>
      <InputInt
        id="input-int"
        v-model="value"
        label="My int input label"
        description="Some help needed?"
      />
      You typed: {{ JSON.stringify(value) }}
    </demo-item>
    <demo-item>
      <InputInt
        id="input-int-readonly"
        v-model="readonlyModel"
        label="Readonly"
        readonly
      />
      Value: {{ JSON.stringify(readonlyModel) }}
    </demo-item>
</div>
  </template>
  <script>
    export default {
      data: function () {
        return {
          value: null,
          readonlyModel: 32
        };
      },
    };
  </script>
</docs>
