<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    :errorMessage="errorMessage"
  >
    <InputGroup>
      <input
        :id="id"
        :ref="id"
        v-model="input"
        type="text"
        class="form-control"
        :aria-describedby="id"
        placeholder="Search"
      />
      <template v-slot:append>
        <button
          v-if="isClearBtnShown"
          @click="input = null"
          class="btn btn-outline-primary"
          type="button"
        >
          <i class="fas fa-fw fa-times"></i>
        </button>
      </template>
    </InputGroup>
  </FormGroup>
</template>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";
import InputGroup from "./InputGroup.vue";

export default {
  name: "InputSearch",
  extends: BaseInput,
  components: { FormGroup, InputGroup },
  props: {
    debounceTime: {
      type: Number,
      required: false,
      default: () => 300,
    },
    isClearBtnShown: {
      type: Boolean,
      required: false,
      default: () => false,
    },
  },
  data() {
    return {
      timeout: null,
      debouncedInput: this.modelValue,
    };
  },
  computed: {
    input: {
      get() {
        return this.debouncedInput;
      },
      set(val) {
        if (this.timeout) {
          clearTimeout(this.timeout);
        }
        this.timeout = setTimeout(() => {
          this.debouncedInput = val;
          this.$emit("update:modelValue", this.debouncedInput);
        }, this.debounceTime);
      },
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label class="font-italic">Basic search field ( with default 300ms debounce)</label>
    <demo-item>
      <InputSearch
          id="input-search-1"
          v-model="value1"
      />
      <div>You search: {{ value1 }}</div>
    </demo-item>
    <label class="font-italic">Pre filled search value and clear button</label>
    <demo-item>
      <InputSearch
          id="input-search-2"
          v-model="value2"
          :isClearBtnShown="true"
      />
      <div>You search: {{ value2 }}</div>
    </demo-item>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value1: "",
        value2: "apples",
      };
    }
  };
</script>
</docs>
