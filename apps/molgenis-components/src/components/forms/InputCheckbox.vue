<template>
  <FormGroup
    :id="id"
    :label="label"
    :description="description"
    class="checkbox-form-group"
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
            'input',
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
      class="checkbox-clear-value btn-link btn m-0 p-0"
      @click.prevent="
        result = [];
        $emit('input', result);
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
import BaseInput from "./BaseInput.vue";
import FormGroup from "./FormGroup.vue";

export default {
  extends: BaseInput,
  components: {
    FormGroup,
  },
  props: {
    options: Array,
  },
  data() {
    return {
      result: this.value ? [...this.value] : [],
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
