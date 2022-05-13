<template>
  <FormGroup :id="id + '-from'" :label="label" :description="description">
    <InputGroup class="d-flex">
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>
      <BaseInputDecimal
        :id="id + '-from'"
        :value="value[0]"
        @input="emitValue($event, 0)"
        placeholder="from"
      />
      <BaseInputDecimal
        :id="id + '-to'"
        :value="value[1]"
        @input="emitValue($event, 1)"
        placeholder="to"
      />
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
    value: {
      type: Array,
      default: () => [null, null],
    },
  },
  methods: {
    emitValue(event, index) {
      let result = [...this.value];
      result[index] = event;
      this.$emit("input", result);
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
