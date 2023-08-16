<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="stringError"
  >
    <InputGroup>
      <template v-slot:prepend>
        <slot name="prepend"></slot>
      </template>
      <input
        :id="id"
        :ref="id"
        :name="name"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        type="text"
        class="form-control"
        :class="{ 'is-invalid': stringError }"
        :aria-describedby="id"
        :placeholder="placeholderValue"
        :readonly="readonly"
      />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  name: "InputString",
  components: { FormGroup, InputGroup },
  extends: BaseInput,
  props: {
    stringLength: {
      type: Number,
      default: 255,
    },
    additionalValidValidationStrings: {
      type: Array,
      default: [],
    },
  },
  methods: {
    validateEmail(email) {
      return (
        this.additionalValidValidationStrings.includes(email) ||
        email?.match(
          /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
        )
      );
    },
  },
  computed: {
    stringError() {
      if (this.modelValue?.length > this.stringLength) {
        return `Please limit to ${this.stringLength} characters.`;
      } else if (!this.validateEmail(this.modelValue)) {
        return `Please enter a valid email address`;
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
      <InputEmail id="input-email1" v-model="value" label="My email input label" description="Some help needed?"/>
      You typed: {{ JSON.stringify(value) }}<br/>
      <b>Readonly</b>
      <InputEmail id="input-email2" :readonly="true" value="info@molgenis.org"
                   description="Should not be able to edit this"/>
      <b>additionalValidValidationStrings: user, admin, anonymous</b>
      <InputEmail id="input-email2" v-model="value" :additionalValidValidationStrings="['user', 'admin', 'anonymous']"
                   description="validates email addresses with additional valid 'user', 'admin', 'anonymous' strings "/>
      <b>string length</b>
      <InputEmail id="input-email5" v-model="value" :stringLength="4" label="maximum stringLength (4)"/>
    </div>
  </template>
  <script>
    export default {
      data: function () {
        return {
          value: "info@molgenis.org",
        };
      }
    };
  </script>
  </docs>
