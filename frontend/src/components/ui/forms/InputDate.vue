<template>
  <form-group v-bind="$props">
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx + valueArray.length"
      v-bind="$props"
      :show-clear="showClear(idx)"
      :show-minus="showMinus(idx)"
      :show-plus="showPlus(idx)"
      @add="addRow"
      @clear="clearValue(idx)"
    >
      <input
        v-if="readonly"
        v-model="valueArray[idx]"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
        readonly
      >
      <FlatPickr
        v-else
        v-model="valueArray[idx]"
        class="form-control active"
        :class="{ 'is-invalid': errorMessage }"
        :config="config"
        :disabled="readonly"
        :placeholder="placeholder"
        style="background: #fff;"
        @input="emitValue($event, idx)"
      />
    </InputAppend>
  </form-group>
</template>

<script>
import 'flatpickr/dist/flatpickr.css'
import _baseInput from './_baseInput.vue'
import FlatPickr from 'vue-flatpickr-component'
import InputAppend from './_inputAppend.vue'

/** Show a data input */
export default {
  components: {
    FlatPickr,
    InputAppend,
  },
  extends: _baseInput,
  computed: {
    config() {
      return {
        allowInput: false,
        clickOpens: !this.readonly,
        dateFormat: 'Y-m-d',
        wrap: true, // set wrap to true only when using 'input-group'
      }
    },
  },
}
</script>
