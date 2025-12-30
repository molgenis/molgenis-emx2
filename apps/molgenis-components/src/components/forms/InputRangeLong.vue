<template>
  <FormGroup
    :id="id + '-from'"
    :label="label"
    :description="description"
    :errorMessage="errorMessage"
  >
    <InputGroup class="d-flex">
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>
      <BaseInputLong
        :id="id + '-from'"
        :modelValue="modelValue[0]"
        @update:modelValue="emitValue($event, 0)"
        placeholder="from"
        :class="{ 'is-invalid': errorMessage }"
      />
      <BaseInputLong
        :id="id + '-to'"
        :modelValue="modelValue[1]"
        @update:modelValue="emitValue($event, 1)"
        placeholder="to"
        :class="{ 'is-invalid': errorMessage }"
      />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import BaseInputLong from "./baseInputs/BaseInputLong.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  components: { BaseInputLong, FormGroup, InputGroup },
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
      result[index] = value === "" ? null : value;
      this.$emit("update:modelValue", result);
    },
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <InputRangeLong
        id="input-range-int"
        v-model="value"
        description="Normal range input"
        label="Range Integer input"
      />
      You've entered: {{ value }}
    </DemoItem>
    <DemoItem>
      <InputRangeLong
        id="input-range-int-default"
        v-model="defaultValue"
        description="Range input with default"
        label="Default Range Integer input"
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
      defaultValue: [1,2],
    };
  },
};
</script>
</docs>
