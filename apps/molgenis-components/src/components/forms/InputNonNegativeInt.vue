<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="nonNegativeIntError"
  >
    <InputGroup>
      <BaseInputInt
        :id="id"
        :modelValue="modelValue"
        :placeholder="placeholder"
        :readonly="readonly"
        :required="required"
        :class="{ 'is-invalid': nonNegativeIntError }"
        @update:modelValue="$emit('update:modelValue', $event)"
      />
      <template v-slot:append>
        <slot name="append" />
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script setup lang="ts">
import BaseInputInt from "./baseInputs/BaseInputInt.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import {
  isInvalidNonNegativeInt,
  NON_NEGATIVE_INT_ERROR,
} from "./formUtils/formUtils";
import { computed } from "vue";

const props = defineProps<{
  id: string;
  label?: string;
  required?: boolean;
  description?: string;
  errorMessage?: string;
  placeholder?: string;
  readonly?: boolean;
}>();

const modelValue = defineModel<Number>("modelValue", {
  required: true,
});

const nonNegativeIntError = computed(() => {
  if (typeof modelValue.value === "number") {
    if (isInvalidNonNegativeInt(modelValue.value)) {
      return NON_NEGATIVE_INT_ERROR;
    } else {
      return props.errorMessage;
    }
  } else {
    return props.errorMessage;
  }
});
</script>

<docs>
<template>
  <div>
    <demo-item>
      <InputNonNegativeInt
          id="input-non-negative-int"
          v-model="value"
          label="My non negative int input label"
          description="Some help needed?"
      />
      You typed: {{ JSON.stringify(value) }}
    </demo-item>
    <demo-item>
      <InputNonNegativeInt
          id="input-non-negative-int-readonly"
          v-model="readonlyModel"
          label="Readonly"
          readonly
      />
      Value: {{ JSON.stringify(readonlyModel) }}
    </demo-item>
    <demo-item>
      <InputNonNegativeInt
          id="input-non-negative-int-negative"
          v-model="negativeValue"
          label="My invalid non negative int input label"
      />
      Value: {{ JSON.stringify(negativeValue) }}
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function() {
      return {
        value: 0,
        readonlyModel: 42,
        negativeValue: -1,
      };
    },
  };
</script>
</docs>
