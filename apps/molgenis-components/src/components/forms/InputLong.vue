<template>
  <span>
    <FormGroup
      :id="id"
      :label="label"
      :description="description"
      :errorMessage="longError"
      v-on="$listeners"
    >
      <input
        :value="value"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
        :aria-describedby="id + 'Help'"
        :placeholder="placeholder"
        :readonly="readonly"
        @keypress="handleKeyValidity($event)"
        @input="inputHandler($event)"
      />
    </FormGroup>
  </span>
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
    errorMessage: { type: String, default: null },
    readonly: {
      type: Boolean,
      required: false,
      default: () => undefined,
    },
  },
  computed: {
    longError() {
      return getBigIntError(this.value);
    },
  },
  methods: {
    handleKeyValidity(event) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) this.flipSign();
      if (!isNumericKey(event)) event.preventDefault();
    },
    flipSign() {
      if (this.value && this.value.length > 0) {
        if (this.value.charAt(0) === "-") {
          this.$emit("input", this.value.substring(1));
        } else {
          this.$emit("input", "-" + this.value);
        }
      }
    },
    inputHandler(event) {
      const value = event.target.value;
      if (value.length > 0) {
        this.$emit("input", value);
      } else {
        this.$emit("input", null);
      }
    },
  },
};

const BIG_INT_ERROR = `Invalid value: must be value from ${MIN_LONG} to ${MAX_LONG}`;

function getBigIntError(value) {
  if (isInvalidBigInt(value)) {
    return BIG_INT_ERROR;
  }
  return undefined;
}

function isInvalidBigInt(value) {
  return BigInt(value) > BigInt(MAX_LONG) || BigInt(value) < BigInt(MIN_LONG);
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
