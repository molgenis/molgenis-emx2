<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    :errorMessage="errorMessage"
  >
    <input
      :id="id"
      :value="value"
      :class="{ 'form-control': true, 'is-invalid': errorMessage }"
      :aria-describedby="id + 'Help'"
      :placeholder="placeholder"
      :readonly="readonly"
      @keypress="handleKeyValidity($event)"
      @input="inputHandler($event)"
    />
  </FormGroup>
</template>

<script>
import FormGroup from "./FormGroup.vue";
import BaseInput from "./BaseInput.vue";
import constants from "../constants";
import { isNumericKey } from "./utils/InputUtils";

const { CODE_MINUS, MIN_LONG, MAX_LONG } = constants;

export default {
  extends: BaseInput,
  components: {
    FormGroup,
  },
  props: {
    readonly: {
      type: Boolean,
      required: false,
      default: () => undefined,
    },
  },
  computed: {
    errorMessage() {
      return getBigIntError(this.value);
    },
  },
  methods: {
    handleKeyValidity(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) {
        this.$emit("input", flipSign(this.value));
      }
      if (!isNumericKey(event)) {
        event.preventDefault();
      }
    },
    inputHandler(event) {
      const value = event.target.value;
      if (value?.length) {
        this.$emit("input", value);
      } else {
        this.$emit("input", null);
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

function flipSign(value) {
  switch (value) {
    case "-":
      return null;
    case null:
      return "-";
    default:
      if (value.charAt(0) === "-") {
        return value.substring(1);
      } else {
        return "-" + value;
      }
  }
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
    <demo-item>
      <div>
        <InputLong id="input-long" v-model="value" label="My long input label" description="Some help needed?"/>
        You typed: {{ JSON.stringify(value) }}
      </div>
    </demo-item>
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
