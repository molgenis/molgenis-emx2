<template>
  <FormGroup :id="id" :label="label" :description="description">
    <BaseInputDate
      :id="id"
      :value="value"
      :placeholder="placeholder"
      :readonly="readonly"
      @input="$emit('input', $event)"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import BaseInputDate from "./baseInputs/BaseInputDate.vue";
import FormGroup from "./FormGroup.vue";

export default {
  extends: BaseInput,
  components: {
    BaseInputDate,
    FormGroup,
  },
  props: {
    readonly: { type: Boolean, default: false },
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