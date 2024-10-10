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
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import BaseInputProps from "./baseInputs/BaseInputProps";
import constants from "../constants";

let props = defineProps({
  ...BaseInputProps,
  modelValue: {
    type: String,
    default: null,
  },
});

const emit = defineEmits(["update:modelValue"]);

const stringError = computed(() => {
  if (typeof props.modelValue === "string") {
    if (!validateHyperlink(props.modelValue)) {
      return `Please enter a valid hyperlink`;
    } else {
      return props.errorMessage;
    }
  }
});

function validateHyperlink(hyperlink: string) {
  return hyperlink?.match(constants.HYPERLINK_REGEX);
}

function updateModelValue(value) {
  emit("update:modelValue", value === "" ? null : value);
}
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
