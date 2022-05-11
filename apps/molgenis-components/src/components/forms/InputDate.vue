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
        wrap: true,
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