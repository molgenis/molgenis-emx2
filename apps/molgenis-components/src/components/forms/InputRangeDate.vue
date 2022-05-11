<template>
  <FormGroup :id="id" :label="label" :description="description">
    <InputDate
      :id="id + '-from'"
      :value="value[0]"
      :readonly="readonly"
      @input="emitValue($event, 0)"
      placeholder="from"
      class="m-0 d-inline-block"
    />
    <InputDate
      :id="id + '-to'"
      :value="value[1]"
      :readonly="readonly"
      @input="emitValue($event, 1)"
      placeholder="to"
      class="m-0 d-inline-block"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import InputDate from "./InputDate.vue";
import FormGroup from "./FormGroup.vue";

export default {
  components: { InputDate, FormGroup },
  extends: BaseInput,
  props: {
    value: {
      type: Array,
      default: () => [null, null],
    },
    readonly: { type: Boolean, default: false },
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
