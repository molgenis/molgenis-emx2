<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="stringError"
  >
    <InputGroup>
      <input
        :id="id"
        :ref="id"
        :name="name"
        :value="modelValue"
        @input="
          //@ts-ignore
          $emit('update:modelValue', $event.target?.value)
        "
        type="text"
        class="form-control"
        :class="{ 'is-invalid': stringError }"
        :aria-describedby="id"
        :placeholder="placeholder"
        :readonly="readonly"
      />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script lang="ts">
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import constants from "../constants";

export default {
  name: "InputHyperlink",
  components: { FormGroup, InputGroup },
  extends: BaseInput,
  computed: {
    stringError() {
      if (typeof this.modelValue === "string") {
        if (!this.validateHyperlink(this.modelValue)) {
          return `Please enter a valid hyperlink`;
        } else {
          return this.errorMessage;
        }
      } else {
        return this.errorMessage;
      }
    },
  },
  methods: {
    validateHyperlink(hyperlink: string) {
      return hyperlink?.match(constants.HYPERLINK_REGEX);
    },
  },
};
</script>

<style scoped>
.is-invalid {
  background-image: none;
}

span:hover .hoverIcon {
  visibility: visible;
}
</style>

<docs>
<template>
  <div>
    <InputHyperlink
      id="input-hyperlink1"
      v-model="value"
      label="My hyperlink input label"
      description="Some help needed?"
    />
    You typed: {{ JSON.stringify(value) }}<br />
    <b>Readonly</b>
    <InputHyperlink
      id="input-hyperlink2"
      :readonly="true"
      v-model="value"
      description="Should not be able to edit this"
    />
  </div>
</template>
<script setup>
import { ref } from "vue"
const value = ref("https://www.molgenis.org")
</script>
</docs>
