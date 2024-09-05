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
          updateModelValue($event.target?.value)
        "
        type="text"
        class="form-control"
        :class="{ 'is-invalid': stringError }"
        :aria-describedby="id"
        :placeholder="placeholder"
        :readonly="readonly"
      />
    </InputGroup>
  </FormGroup>
</template>

<script setup lang="ts">
import { computed } from "vue";
import constants from "../constants";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import BaseInputProps from "./baseInputs/BaseInputProps";

let props = defineProps({
  ...BaseInputProps,
  modelValue: {
    type: String,
    default: null,
  },
  stringLength: {
    type: Number,
    default: 255,
  },
  additionalValidValidationStrings: {
    type: Array,
    default: [],
  },
});

const emit = defineEmits(["update:modelValue"]);

function updateModelValue(value) {
  emit("update:modelValue", value == "" ? null : value);
}

function validateEmail(email: string) {
  return (
    props.additionalValidValidationStrings.includes(email) ||
    email?.match(constants.EMAIL_REGEX)
  );
}

const stringError = computed(() => {
  if (typeof props.modelValue === "string") {
    if (props.modelValue.length > props.stringLength) {
      return `Please limit to ${props.stringLength} characters.`;
    } else if (!validateEmail(props.modelValue)) {
      return `Please enter a valid email address`;
    } else {
      return props.errorMessage;
    }
  }
});
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
    <InputEmail
      id="input-email1"
      v-model="value"
      label="My email input label"
      description="Some help needed?"
    />
    You typed: {{ JSON.stringify(value) }}<br />
    <b>Readonly</b>
    <InputEmail
      id="input-email2"
      :readonly="true"
      value="info@molgenis.org"
      description="Should not be able to edit this"
    />
    <b>additionalValidValidationStrings: user, admin, anonymous</b>
    <InputEmail
      id="input-email3"
      v-model="value"
      :additionalValidValidationStrings="['user', 'admin', 'anonymous']"
      description="validates email addresses with additional valid 'user', 'admin', 'anonymous' strings "
    />
    <b>string length</b>
    <InputEmail
      id="input-email5"
      v-model="value"
      :stringLength="8"
      label="maximum stringLength (4)"
    />
  </div>
</template>
<script>
export default {
  data: function () {
    return {
      value: "info@molgenis.org",
    };
  },
};
</script>
</docs>
