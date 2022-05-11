<template>
  <FormGroup :id="id" :label="label" :description="description">
    <InputDateTime
      :id="id + '-from'"
      :value="value[0]"
      :readonly="readonly"
      @input="emitValue($event, 0)"
      placeholder="from"
      class="m-0 d-inline-block"
    />
    <InputDateTime
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
import InputRangeDate from "./InputRangeDate.vue";
import InputDateTime from "./InputDateTime.vue";
import FormGroup from "./FormGroup.vue";

export default {
  components: { InputDateTime, FormGroup },
  extends: InputRangeDate,
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <InputRangeDateTime
        id="input-range-date-time"
        v-model="value"
        description="Normal range input"
        label="Range date time input"
      />
      You've entered: {{ value }}
    </DemoItem>
    <DemoItem>
      <InputRangeDateTime
        id="input-range-date-time-default"
        v-model="defaultValue"
        description="Range input with default"
        label="Default range date time input"
      />
      You've entered: {{ defaultValue }}
    </DemoItem>
    <DemoItem>
      <InputRangeDateTime
        id="input-range-date-time-read-only"
        v-model="readonlyValue"
        description="Range input with read only default"
        label="Read only range date time input"
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
      defaultValue: ["1970-01-01T20:37:37", "2042-12-12T00:11:12"],
      readonlyValue: ["1999-01-01T00:11:12", "2022-01-13T00:12:13"],
    };
  },
};
</script>
</docs>
