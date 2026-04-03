<script>
import InputDate from "./InputDate.vue";

export default {
  name: "InputDateTime",
  extends: InputDate,
  computed: {
    config() {
      return {
        wrap: true,
        dateFormat: "Y-m-dTH:i:S",
        allowInput: false,
        enableTime: true,
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
      <InputDateTime
        id="input-date-time"
        v-model="value"
        label="Input Date/Time"
        description="Normal input date/time"
      />
      <div>You selected: {{ value }}</div>
    </DemoItem>
    <DemoItem>
      <InputDateTime
        id="input-date-time-readonly"
        v-model="readonlyValue"
        readonly
        label="Input Date/Time - readonly"
        description="Readonly input date/time"
      />
    </DemoItem>
    <DemoItem>
      <InputDateTime
        id="input-date-time-default"
        v-model="defaultValue"
        label="Input Date/Time - defaultvalue"
        description="Input date/time with a default value"
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
      defaultValue: "2022-1-1T00:25:00",
      readonlyValue: "2022-1-1T13:37:42",
    };
  },
};
</script>
</docs>
