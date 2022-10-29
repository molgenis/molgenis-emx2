<template>
  <div v-if="inplace">
    <input
      :id="id"
      :checked="value"
      type="checkbox"
      :aria-describedby="id + 'Help'"
      @change="$emit('input', $event.target.checked)"
    />
    <label class="ml-1" :for="id">{{ label }}</label>
  </div>
  <InputRadio
    v-else
    v-bind="$attrs"
    :value="value"
    :options="[true, false]"
    :isClearable="isClearable"
    @input="$emit('input', value)"
  />
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
        <InputBoolean id="input-boolean" v-model="value" label="My first boolean" description="do you need some boolean help?"/>
      </DemoItem>
      <DemoItem>
        <p>InputBoolean:</p>
        <InputBoolean id="input-boolean2" v-model="secondValue" :isClearable="false" label="real boolean" description="can not be cleared"/>
      </DemoItem>
      <DemoItem>
        <p>InputBoolean - inplace:</p>
        <InputBoolean id="input-boolean-inplace" :inplace="true" v-model="value" label="My inplace boolean"
                      description="do you need some boolean help?"/>
      </DemoItem>
      <div>value: {{ value }}</div>
      <div>non clearable value: {{ secondValue }}</div>
    </div>
  </template>
  <script>
    export default {
      data() {
        return {
          value: true,
          secondValue: false
        }
      }
    }
  </script>
</docs>
