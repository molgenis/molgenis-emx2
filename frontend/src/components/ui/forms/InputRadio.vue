<template>
  <form-group v-bind="$props" class="radio-form-group">
    <div>
      <div
        v-for="(item, idx) in options"
        :key="idx"
        class="form-check form-check-inline"
        :class="{ 'is-invalid': errorMessage }"
      >
        <input
          :id="id + idx"
          v-model="valueArray[0]"
          :aria-describedby="id + 'Help'"
          :checked="valueArray[0] == item"
          class="form-check-input"
          type="radio"
          :value="item"
          @change="emitValue($event, idx)"
        >
        <label class="form-check-label" :for="id + idx">{{ item }}</label>
      </div>
      <a
        v-if="valueArray[0] !== null"
        class="radio-clear-value"
        href="#"
        @click.prevent="clearValue(0)"
      >
        clear
      </a>
    </div>
  </form-group>
</template>

<script>
import InputSelect from './InputSelect.vue'

export default {
  extends: InputSelect,
}
</script>

<style>
.radio-clear-value {
  display: none;
}

.radio-form-group:hover .radio-clear-value {
  display: inline;
}
</style>
