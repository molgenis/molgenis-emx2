<template>
    <span>
        <span v-if="inplace && !focus && !errorMessage" @click="toggleFocus">
            <span v-if="list && value">{{ value.join(", ") }}</span>
            <span v-else> {{ value ? value : "&zwnj;&zwnj;" }}</span>
        </span>
        <FormGroup v-else v-bind="$props">
            <InputAppend
                v-for="(item, idx) in valueArray"
                :key="idx"
                v-bind="$props"
                :show-clear="showClear(idx)"
                :show-minus="showMinus(idx)"
                :show-plus="showPlus(idx)"
                @add="addRow"
                @clear="clearValue(idx)"
            >
                <input
                    v-focus="inplace && !list"
                    :aria-describedby="id + 'Help'"
                    :class="{ 'form-control': true, 'is-invalid': errorMessage }"
                    :placeholder="placeholder"
                    :readonly="readonly"
                    :value="item"
                    @blur="toggleFocus"
                    @input="emitValue($event, idx)"
                    @keypress="keyhandler"
                >
            </InputAppend>
        </FormGroup>
        <IconAction
            v-if="inplace"
            class="hoverIcon"
            :icon="focus ? 'times' : 'pencil'"
            @click="toggleFocus"
        />
    </span>
</template>

<script>
import BaseInput from "./_baseInput.vue";
import InputAppend from "./_inputAppend";
import FormGroup from "./_formGroup";
import IconAction from "./IconAction";

export default {
  components: {
    InputAppend,
    FormGroup,
    IconAction,
  },
  extends: BaseInput,
  methods: {
    keyhandler(event) {
      return event;
    },
  },
};
</script>

<style scoped>
.is-invalid {
  background-image: none;
}

span:hover .hoverIcon {
  visibility: visible;
}
</style>
