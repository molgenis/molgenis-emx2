<template>
  <FormGroup :id="id" :label="label" :description="description">
    <input v-if="readonly" readonly v-model="value" class="form-control" />
    <FlatPickr
      v-else
      :value="value"
      style="background: white"
      class="form-control active"
      :config="config"
      :placeholder="placeholder"
      :disabled="readonly"
      @input="$emit('input', $event)"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import FlatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.css";

/** Show a data input */
export default {
  extends: BaseInput,
  components: {
    FlatPickr,
    FormGroup,
  },
  props: {
    readonly: { type: Boolean, default: false },
  },
  computed: {
    config() {
      return {
        wrap: true, // set wrap to true only when using 'input-group'
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
      <p>Input Date</p>
      <InputDate id="input-date" v-model="value" label="My date input label" description="Some help needed?"/>
      <br/>
      You selected: {{ value }}
    </DemoItem>
    <DemoItem>
      <p>Input Date - readonly</p>
      <InputDate id="input-date" v-model="defaultValue" readonly label="My date input label" description="Some help needed?"/>
    </DemoItem>
    <DemoItem>
      <p>Input Date - defaultvalue</p>
      <InputDate id="input-date" v-model="defaultValue" label="My date input label" description="Some help needed?"/>
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
        defaultValue:'2022-1-1'
      };
    }
  };
</script>
</docs>
