<template>
  <FormGroup
    :id="id + '-from'"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage">
    <InputGroup class="d-flex">
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>
      <BaseInputDecimal
        :id="id + '-from'"
        :modelValue="modelValue[0]"
        @update:modelValue="emitValue($event, 0)"
        placeholder="from" />
      <BaseInputDecimal
        :id="id + '-to'"
        :modelValue="modelValue[1]"
        @update:modelValue="emitValue($event, 1)"
        placeholder="to" />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import BaseInputDecimal from "./baseInputs/BaseInputDecimal.vue";
import InputGroup from "./InputGroup.vue";

export default {
  components: { FormGroup, BaseInputDecimal, InputGroup },
  extends: BaseInput,
  props: {
    modelValue: {
      type: Array,
      default: () => [null, null],
    },
  },
  methods: {
    emitValue(value, index) {
      let result = [...this.modelValue];
      result[index] = value;
      this.$emit("update:modelValue", result);
    },
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <InputRangeDecimal
        id="input-range-decimal"
        v-model="value"
        description="Normal range input"
        label="Range decimal input"
      />
      You've entered: {{ value }}
    </DemoItem>
    <DemoItem>
      <InputRangeDecimal
        id="input-range-decimal-default"
        v-model="defaultValue"
        description="Range input with default"
        label="Default range decimal input"
      />
      You've entered: {{ defaultValue }}
    </DemoItem>
  </div>
</template>
<script>
export default {
  data() {
    return {
      value: undefined,
      defaultValue: [13.37, 4.2],
    };
  },
};
</script>
</docs>
