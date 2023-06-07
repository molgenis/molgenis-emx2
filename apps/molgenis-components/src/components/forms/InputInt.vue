<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage">
    <InputGroup>
      <BaseIntInput
        :id="id"
        :modelValue="modelValue"
        :placeholder="placeholder"
        :readonly="readonly"
        :required="required"
        :class="{ 'is-invalid': errorMessage }"
        @update:modelValue="
          $emit('update:modelValue', $event === NaN ? null : parseInt($event))
        " />
      <template v-slot:append>
        <slot name="append" />
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import BaseIntInput from "./baseInputs/BaseInputInt.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    BaseIntInput,
    InputGroup,
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
    data: function() {
      return {
        value: 0,
        readonlyModel: 32,
      };
    },
  };
</script>
</docs>
