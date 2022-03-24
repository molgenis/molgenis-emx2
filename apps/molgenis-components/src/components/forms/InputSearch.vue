<template>
  <div class="form-group">
    <label v-if="label !== null && label !== undefined" :for="id">{{
      label
    }}</label>
    <input
      :id="id"
      :ref="id"
      v-model="input" 
      type="text"
      class="form-control"
      :aria-describedby="id"
      placeholder="Search"
    />
    <small
      v-if="isNonEmptyString(helpText)"
      :id="id + '-help-text'"
      class="form-text text-muted"
      >{{ helpText }}</small
    >
  </div>
</template>

<script>
import BaseInput from './BaseInput.vue';

export default {
  name: 'InputSearch',
  extends: BaseInput,
  props: {
    debounceTime: {
      type: Number,
      required: false,
      default: () => 300
    }
  },
  data () {
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
          this.$emit('input', this.debouncedInput)
        }, this.debounceTime);
      }
    }
  }
};
</script>