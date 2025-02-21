<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="stringError"
  >
    <InputGroup>
      <textarea
        :id="id"
        :ref="id"
        :name="name"
        :value="modelValue"
        @input="
          $emit('update:modelValue', ($event.target as HTMLInputElement).value)
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

<script lang="ts">
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";
import BaseInput from "./baseInputs/BaseInput.vue";
import { isJsonObjectOrArray } from "./formUtils/formUtils";

export default {
  name: "InputJson",
  components: { FormGroup, InputGroup },
  extends: BaseInput,
  props: {
    modelValue: {
      type: [String, null],
      default: null,
    },
  },
  computed: {
    stringError() {
      if (typeof this.modelValue === "string") {
        try {
          if (!isJsonObjectOrArray(JSON.parse(this.modelValue))) {
            return `Root element must be an object or array`;
          }
        } catch {
          return `Please enter valid JSON`;
        }
        return this.errorMessage;
      } else {
        return this.errorMessage;
      }
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
    <InputJson
      id="input-json"
      v-model="value"
      label="My JSON input label"
      description="Some help needed?"
    />
    You typed: {{ value }}<br />
    <b>Readonly</b>
    <InputJson
      id="input-json2"
      :readonly="true"
      v-model="readOnlyValue"
      description="Should not be able to edit this"
    />
  </div>
</template>
<script>
export default {
  data: function () {
    return {
      value: '{"name":"bofke"}',
      readOnlyValue: '{"name":"bofke"}',
    };
  },
};
</script>
</docs>
