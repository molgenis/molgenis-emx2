<script>
import InputDate from "./InputDate.vue";

export default {
  name: "InputDateTime",
  extends: InputDate,
  computed: {
    config() {
      return {
        wrap: true, // set wrap to true only when using 'input-group'
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
      <p>Input Date/Time</p>
      <InputDateTime id="input-date" v-model="value" label="My date input label" description="Some help needed?"/>
      <br/>
      You selected: {{ value }}
    </DemoItem>
    <DemoItem>
      <p>Input Date/Time - readonly</p>
      <InputDateTime id="input-date" v-model="defaultValue" readonly label="My date input label" description="Some help needed?"/>
    </DemoItem>
    <DemoItem>
      <p>Input Date/Time - defaultvalue</p>
      <InputDateTime id="input-date" v-model="defaultValue" label="My date input label" description="Some help needed?"/>
      <br/>
      You selected: {{ defaultValue }}
    </DemoItem>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: null,
        defaultValue:'2022-1-1T12:15:00'
      };
    }
  };
</script>
</docs>
