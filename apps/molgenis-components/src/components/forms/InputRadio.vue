<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    class="radio-form-group"
  >
    <div
      class="input-group"
      @mouseenter="isMouseOver = true"
      @mouseleave="isMouseOver = false"
    >
      <div
        v-for="(item, idx) in options"
        :key="idx"
        class="form-check form-check-inline"
      >
        <input
          class="form-check-input"
          type="radio"
          :id="id + idx"
          :aria-describedby="id + 'Help'"
          :value="item"
          v-model="radioValue"
        />
        <label class="form-check-label" :for="id + idx">{{ item }}</label>
      </div>
      <div class="input-group-append">
        <button
          v-show="isClearShown"
          class="btn btn-link radio-clear-value"
          @click="radioValue = null"
        >
          clear
        </button>
      </div>
    </div>
  </FormGroup>
</template>

<script>
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";

export default {
  name: "InputRadio",
  extends: BaseInput,
  components: { FormGroup },
  props: {
    options: {
      type: Array,
      required: true,
    },
  },
  data() {
    return {
      radioValue: this.value,
      isMouseOver: false,
    };
  },
  watch: {
    radioValue() {
      this.$emit("input", this.radioValue);
    },
  },
  computed: {
    isClearShown() {
      return (
        this.isMouseOver &&
        this.radioValue !== null &&
        this.radioValue != undefined
      );
    },
  },
};
</script>

<style scoped>
.input-group-append button {
  padding-top: 0;
  padding-bottom: 0;
  border: 0;
}
</style>

<docs>
<template>
  <div>
    <label>Basic example</label>
    <demo-item>
      <InputRadio
          id="input-radio-1"
          v-model="value1"
          :options="['options 1', 'option 2']"
          label="My radio input label"
          description="Some help needed?"
      />
      <div>You selected: {{ value1 }}</div>
    </demo-item>

    <label>Example with defaultValue</label>
    <demo-item>
      <InputRadio
          id="input-radio-2"
          v-model="value2"
          :options="['option 1', 'option 2']"
          label="My radio input label"
      />
      <div>You selected: {{ value2 }}</div>
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value1: null,
        value2: 'option 1',
      };
    },
  };
</script>
</docs>
