<template>
  <FormGroup :id="id" :label="label" :description="description">
    <input
      :id="id"
      :ref="id"
      v-model="input"
      type="text"
      class="form-control"
      :aria-describedby="id"
      placeholder="Search"
    />
  </FormGroup>
</template>

<script>
import BaseInput from './BaseInput.vue';
import FormGroup from './FormGroup.vue';

export default {
  name: 'InputSearch',
  extends: BaseInput,
  components: {FormGroup},
  props: {
    debounceTime: {
      type: Number,
      required: false,
      default: () => 300
    }
  },
  data() {
    return {
      timeout: null,
      debouncedInput: this.value
    };
  },
  computed: {
    input: {
      get() {
        return this.debouncedInput;
      },
      set(val) {
        if (this.timeout) clearTimeout(this.timeout);
        this.timeout = setTimeout(() => {
          this.debouncedInput = val;
          this.$emit('input', this.debouncedInput);
        }, this.debounceTime);
      }
    }
  }
};
</script>