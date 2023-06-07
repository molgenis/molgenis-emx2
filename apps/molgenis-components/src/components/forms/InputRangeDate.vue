<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    :errorMessage="errorMessage">
    <InputGroup class="d-flex">
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>

      <BaseInputDate
        :id="id + '-from'"
        :modelValue="modelValue[0]"
        :readonly="readonly"
        :config="config"
        @update:modelValue="emitValue($event, 0)"
        placeholder="from"
        class="m-0" />

      <BaseInputDate
        :id="id + '-to'"
        :modelValue="modelValue[1]"
        :readonly="readonly"
        :config="config"
        @update:modelValue="emitValue($event, 1)"
        placeholder="to"
        class="m-0" />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import BaseInputDate from "./baseInputs/BaseInputDate.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  components: { BaseInputDate, FormGroup, InputGroup },
  extends: BaseInput,
  props: {
    modelValue: {
      type: Array,
      default: () => [null, null],
    },
    readonly: { type: Boolean, default: false },
  },
  methods: {
    emitValue(value, index) {
      let result = [...this.modelValue];
      result[index] = value;
      this.$emit("update:modelValue", result);
    },
  },
  computed: {
    config() {
      return {
        wrap: false,
        dateFormat: "Y-m-d",
        allowInput: false,
        clickOpens: !this.readonly,
      };
    },
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <InputRangeDate
        id="input-range-date"
        v-model="value"
        description="Normal range input"
        label="Range Date input"
      />
      You've entered: {{ value }}
    </DemoItem>
    <DemoItem>
      <InputRangeDate
        id="input-range-date-default"
        v-model="defaultValue"
        description="Range input with default"
        label="Default range Date input"
      />
      You've entered: {{ defaultValue }}
    </DemoItem>
    <DemoItem>
      <InputRangeDate
        id="input-range-date-read-only"
        v-model="readonlyValue"
        description="Range input with read only default"
        label="Read only range Date input"
        readonly
      />
    </DemoItem>
  </div>
</template>
<script>
export default {
  data() {
    return {
      value: undefined,
      defaultValue: ["1970-01-01", "2042-12-12"],
      readonlyValue: ["1970-01-01", "2042-12-12"],
    };
  },
};
</script>
</docs>
