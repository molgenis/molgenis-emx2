<template>
  <FormGroup :id="id + '-from'" :label="label" :description="description">
    <InputInt
      :id="id + '-from'"
      :value="value[0]"
      @input="emitValue($event, 0)"
      placeholder="from"
      class="m-0 d-inline-block"
    />
    <InputInt
      :id="id + '-to'"
      :value="value[1]"
      @input="emitValue($event, 1)"
      placeholder="to"
      class="m-0 d-inline-block"
    />
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import InputInt from "./InputInt.vue";
import FormGroup from "./FormGroup.vue";

export default {
  components: { InputInt, FormGroup },
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
