<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage || bigIntError">
    <InputGroup>
      <BaseInputLong
        :id="id"
        :modelValue="modelValue"
        :placeholder="placeholder"
        :readonly="readonly"
        :required="required"
        :class="{ 'is-invalid': errorMessage || bigIntError }"
        @update:modelValue="$emit('update:modelValue', $event)" />
      <template v-slot:append>
        <slot name="append" />
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import BaseInputLong from "./baseInputs/BaseInputLong.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import { getBigIntError } from "../utils";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    BaseInputLong,
    InputGroup,
  },
  computed: {
    bigIntError() {
      return getBigIntError(this.modelValue);
    },
  },
};
</script>

<docs>
<template>
  <div>
    <demo-item>
      <div>
        <InputLong id="input-long" v-model="value" label="My long input label" description="Some help needed?"/>
        You typed: {{ JSON.stringify(value) }}
      </div>
    </demo-item>
    <demo-item>
      <div>
        <InputLong id="input-long-read-only" v-model="value" label="Readonly" readonly/>
        Value: {{ JSON.stringify(value) }}
      </div>
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function() {
      return {
        value: "9223372036854775807"
      };
    }
  };
</script>
</docs>
