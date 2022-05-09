<template>
  <div v-if="inplace">
    <input
      :id="id"
      :checked="value"
      type="checkbox"
      :aria-describedby="id + 'Help'"
      @change="$emit('input', $event.target.checked)"
    />
  </div>
  <InputRadio
    v-else
    v-bind="$props"
    :value="value"
    :options="[true, false]"
    @input="$emit('input', value)"
    v-on="$listeners"
  />
</template>

<script>
import BaseInput from "./BaseInput.vue";
import InputRadio from "./InputRadio.vue";

export default {
  components: { InputRadio },
  extends: BaseInput,
  props: {
    inplace: { type: Boolean, default: false },
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
        <p>InputBoolean - inplace:</p>
        <InputBoolean id="input-boolean-inplace" :inplace="true" v-model="value" label="inplace boolean"
                      description="do you need some boolean help?"/>
      </DemoItem>
      <div>value: {{ value }}</div>
    </div>
  </template>
  <script>
    export default {
      data() {
        return {
          value: true
        }
      }
    }
  </script>
</docs>
