<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    :errorMessage="errorMessage || bigIntError"
  >
    <InputGroup>
      <input
        :id="id"
        :value="modelValue"
        class="form-control"
        :class="{ 'is-invalid': errorMessage || bigIntError }"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        :readonly="readonly"
        :required="required"
        @keypress="handleKeyValidity($event)"
        @input="inputHandler($event)"
      />
      <template v-slot:append>
        <slot name="append"></slot>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import FormGroup from "./FormGroup.vue";
import BaseInput from "./baseInputs/BaseInput.vue";
import InputGroup from "./InputGroup.vue";
import constants from "../constants";
import { isNumericKey, flipSign } from "../utils";

const { CODE_MINUS, MIN_LONG, MAX_LONG } = constants;

export default {
  extends: BaseInput,
  components: {
    FormGroup,
    InputGroup,
  },
  props: {
    readonly: {
      type: Boolean,
      required: false,
      default: () => undefined,
    },
  },
  computed: {
    bigIntError() {
      return getBigIntError(this.modelValue);
    },
  },
  methods: {
    handleKeyValidity(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) {
        this.$emit("update:modelValue", flipSign(event.target.value));
      }
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
    inputHandler(event) {
      const value = event.target.value;
      if (value?.length) {
        this.$emit("update:modelValue", value);
      } else {
        this.$emit("update:modelValue", null);
      }
    },
  },
};

const BIG_INT_ERROR = `Invalid value: must be value from ${MIN_LONG} to ${MAX_LONG}`;

function getBigIntError(value) {
  if (value === "-" || isInvalidBigInt(value)) {
    return BIG_INT_ERROR;
  } else {
    return undefined;
  }
}

function isInvalidBigInt(value) {
  return (
    value !== null &&
    (BigInt(value) > BigInt(MAX_LONG) || BigInt(value) < BigInt(MIN_LONG))
  );
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
    <demo-item>
      <div>
        <InputLong id="input-long" v-model="value" label="My long input label" description="Some help needed?"/>
        You typed: {{ JSON.stringify(value) }}
      </div>
    </demo-item>
    <demo-item>
      <div>
        <InputLong id="input-long-read-only" v-model="value" label="Readonly" readonly/>
        Value: {{ JSON.stringify(value) }}
      </div>
    </demo-item>
</div>
  </template>
  <script>
    export default {
      data: function () {
        return {
          value: "9223372036854775807"
        };
      }
    };
  </script>
</docs>
