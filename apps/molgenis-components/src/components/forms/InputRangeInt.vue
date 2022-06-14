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

      <BaseIntInput
        :id="id + '-from'"
        :value="value[0]"
        @input="emitValue($event, 0)"
        placeholder="from"
        :class="{ 'is-invalid': errorMessage }"
      />
      <BaseIntInput
        :id="id + '-to'"
        :value="value[1]"
        @input="emitValue($event, 1)"
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
import BaseIntInput from "./baseInputs/BaseInputInt.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  components: { BaseIntInput, FormGroup, InputGroup },
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
      <InputRangeInt
        id="input-range-int"
        v-model="value"
        description="Normal range input"
        label="Range Integer input"
      />
      You've entered: {{ value }}
    </DemoItem>
    <DemoItem>
      <InputRangeInt
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
      defaultValue: [1, 2],
    };
  },
};
</script>
</docs>
