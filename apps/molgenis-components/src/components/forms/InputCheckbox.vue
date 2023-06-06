<template>
  <FormGroup
    :id="id"
    :label="label"
    :required="required"
    :description="description"
    class="checkbox-form-group"
    :errorMessage="errorMessage"
  >
    <div
      v-for="(option, index) in options"
      :key="index"
      class="form-check form-check-inline"
    >
      <input
        :id="`${id}-${index}`"
        v-model="result"
        class="form-check-input"
        type="checkbox"
        :value="option"
        @change="
          $emit(
            'update:modelValue',
            result.filter((value) => value !== null)
          )
        "
        :aria-describedby="`${id}-help`"
      />
      <label class="form-check-label" :for="`${id}-${index}`">
        {{ option }}
      </label>
    </div>
    <button
      v-if="!hideClearButton"
      class="checkbox-clear-value btn-link btn m-0 p-0"
      @click.prevent="
        result = [];
        $emit('update:modelValue', result);
      "
    >
      clear
    </button>
  </FormGroup>
</template>

<style>
.checkbox-clear-value {
  display: none;
}

.checkbox-form-group:hover .checkbox-clear-value {
  vertical-align: baseline;
  display: inline;
}
</style>

<script>
import BaseInput from "./baseInputs/BaseInput.vue";
import FormGroup from "./FormGroup.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
  },
  props: {
    options: Array,
    hideClearButton: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      result: this.modelValue ? [...this.modelValue] : [],
    };
  },
};
</script>

<docs>
  <template>
    <div>
      <InputCheckbox
          id="animal-checkbox"
          label="Animals"
          v-model="value"
          :options="['lion', 'ape', 'monkey']"
          description="Example checkbox list input"
      />
      Selected: {{ value }}
    </div>
  </template>
  <script>
    export default {
      data: function () {
        return {
          value: ['ape', 'lion']
        };
      }
    };
  </script>
</docs>
