<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage">
    <InputGroup>
      <BaseInputDate
        :id="id"
        :modelValue="modelValue"
        :placeholder="placeholder"
        :readonly="readonly"
        :class="{ 'is-invalid': errorMessage }"
        :required="required"
        :config="config"
        @update:modelValue="$emit('update:modelValue', $event)" />
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
  extends: BaseInput,
  components: {
    BaseInputDate,
    FormGroup,
    InputGroup,
  },
  props: {
    readonly: { type: Boolean, default: false },
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
      <InputDate
        id="input-date"
        v-model="value"
        label="Input Date"
        description="Normal input date"
      />
      <div>You selected: {{ value }}</div>
    </DemoItem>
    <DemoItem>
      <InputDate
        id="input-date-readonly"
        v-model="readonlyValue"
        readonly
        label="Input Date - readonly"
        description="Readonly input date"
      />
    </DemoItem>
    <DemoItem>
      <InputDate
        id="input-date-default"
        v-model="defaultValue"
        label="Input Date - defaultvalue"
        description="Input date with a default value"
      />
      <div>You selected: {{ defaultValue }}</div>
    </DemoItem>
  </div>
</template>
<script>
export default {
  data: function () {
    return {
      value: null,
      defaultValue: "2022-1-1",
      readonlyValue: "2022-1-1",
    };
  },
};
</script>
</docs>
