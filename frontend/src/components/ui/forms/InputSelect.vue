<template>
  <span v-if="inplace">
    <div
      v-if="focus"
      v-click-outside="toggleFocus"
      class="dropdown show"
      style="display: inline-block;"
    >
      <div
        v-for="(el, idx) in valueArray"
        :key="idx"
        class="dropdown-menu show"
      >
        <a
          v-for="option in options"
          :key="option"
          class="dropdown-item"
          :class="{ 'text-primary': option == el }"
          href="#"
          @click.prevent="select(option, idx)"
        >{{ option ? option : "&zwnj;" }}</a>
      </div>
    </div>
    <span
      style="min-width: 1em;"
      @click="toggleFocus"
      @mouseleave="hover = false"
      @mouseover="hover = true"
    >
      {{ value ? value : "&zwnj;&zwnj;" }}
      <IconAction class="hoverIcon" icon="pencil" />
      <div v-if="errorMessage" class="text-danger">{{ errorMessage }}</div>
    </span>
  </span>
  <FormGroup v-else v-bind="$props">
    <InputAppend
      v-for="(el, idx) in valueArray"
      :key="idx + '.' + valueArray.length"
      v-bind="$props"
      :show-clear="showClear(idx)"
      :show-plus="showPlus(idx)"
      @add="addRow"
      @clear="clearValue(idx)"
    >
      <MessageError v-if="!options">
        No options provided
      </MessageError>
      <select
        v-else
        :id="id"
        v-focus="inplace"
        :class="{ 'form-control': true, 'is-invalid': errorMessage }"
        :value="valueArray[idx]"
        @change="emitValue($event, idx)"
      >
        <option v-if="!required" :selected="el === undefined" />
        <option
          v-for="(option, index) in options.filter(
            (o) => el == o || !valueArray.includes(o)
          )"
          :key="index"
          :selected="el == option"
          :value="option"
        >
          {{ option }}
        </option>
      </select>
    </InputAppend>
  </FormGroup>
</template>

<script>
import _baseInput from './_baseInput.vue'
import FormGroup from './_formGroup.vue'
import IconAction from './IconAction.vue'
import InputAppend from './_inputAppend.vue'
import MessageError from './MessageError.vue'

import vClickOutside from 'v-click-outside'

export default {
  directives: {
    clickOutside: vClickOutside.directive,
  },
  components: {
    FormGroup,
    IconAction,
    InputAppend,
    MessageError,
  },
  extends: _baseInput,
  props: {
    options: Array,
  },
  methods: {
    select(option, idx) {
      this.toggleFocus()
      this.emitValue(option, idx)
    },
  },
}
</script>
