<template>
  <div v-if="inplace">
    <input
      :id="id"
      :checked="modelValue"
      type="checkbox"
      :aria-describedby="id + 'Help'"
      @change="$emit('update:modelValue', $event.target.checked)" />
    <label class="ml-1" :for="id">{{ label }}</label>
  </div>
  <InputRadio
    v-else
    v-bind="$props"
    :id="id"
    :modelValue="modelValue"
    :options="[true, false]"
    :isClearable="isClearable"
    @update:modelValue="$emit('update:modelValue', $event)" />
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import InputRadio from "./InputRadio.vue";

export default {
  name: "InputBoolean",
  components: { InputRadio },
  extends: BaseInput,
  props: {
    inplace: { type: Boolean, default: false },
    isClearable: { type: Boolean, default: true },
  },
};
</script>

<docs>
  <template>
    <div>
      <DemoItem>
        <p>InputBoolean:</p>
        <InputBoolean id="input-boolean" v-model="value1" label="My first boolean" description="do you need some boolean help?"/>
        You choose: <div>value: {{ value1 }}</div>
      </DemoItem>
      <DemoItem>
        <p>InputBoolean:</p>
        <InputBoolean id="input-boolean2" v-model="value2" :isClearable="false" label="real boolean" description="can not be cleared"/>
        You choose: <div>value: {{ value2 }}</div>
      </DemoItem>
      <DemoItem>
        <p>InputBoolean - inplace:</p>
        <InputBoolean id="input-boolean-inplace" :inplace="true" v-model="value3" label="My inplace boolean"
                      description="do you need some boolean help?"/>
        You choose: <div>value: {{ value3 }}</div>
      </DemoItem>
      <div>non clearable value: {{ value4 }}</div>
    </div>
  </template>
  <script>
    export default {
      data() {
        return {
          value1: true,
          value2: false,
          value3: true,
          value4: true
        }
      }
    }
  </script>
</docs>
