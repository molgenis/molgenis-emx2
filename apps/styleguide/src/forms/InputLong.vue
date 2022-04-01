<template>
  <span>
    <span v-if="inplace && !focus && !errorMessage" @click="toggleFocus">
      <span v-if="list && value">{{ value.join(", ") }}</span>
      <span v-else> {{ value ? value : "&zwnj;&zwnj;" }}</span>
    </span>
    <FormGroup v-else v-bind="$props" v-on="$listeners">
      <InputAppend
        v-for="(item, idx) in valueArray"
        :key="idx"
        v-bind="$props"
        :showClear="(!inplace || list) && showClear(idx)"
        @clear="clearValue(idx)"
        @add="addRow"
        :showPlus="showPlus(idx)"
        :showMinus="showMinus(idx)"
      >
        <input
          v-focus="inplace && !list"
          :value="item"
          :class="{ 'form-control': true, 'is-invalid': errorMessage }"
          :aria-describedby="id + 'Help'"
          :placeholder="placeholder"
          :readonly="readonly"
          @keypress="keyhandler($event, idx)"
          @input="emitValue($event, idx)"
          @blur="toggleFocus"
        />
      </InputAppend>
    </FormGroup>
    <IconAction
      v-if="inplace && !focus"
      class="hoverIcon"
      icon="pencil-alt"
      @click="toggleFocus"
    />
  </span>
</template>

<script>
import BaseInput from "./_baseInput.vue";
import InputAppend from "./_inputAppend";
import IconAction from "./IconAction";
import { CODE_0, CODE_9, CODE_BACKSPACE, CODE_MINUS } from "../constants";

export default {
  extends: BaseInput,
  components: {
    InputAppend,
    FormGroup: () => import("./_formGroup"), //because it uses itself in nested form
    IconAction,
  },
  methods: {
    keyhandler(event, index) {
      var keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) this.flipSign(index);
      if (!this.isInt(event, keyCode)) event.preventDefault();
    },
    flipSign(index) {
      if (this.value && this.value.length > 0) {
        if (Array.isArray(this.value)) {
          if (this.value[index][0] === "-") {
            this.emitValue(this.value[index].substring(1), index);
          } else {
            this.emitValue("-" + this.value[index], index);
          }
        } else {
          if (this.value[0] === "-") {
            this.emitValue(this.value.substring(1), index);
          } else {
            this.emitValue("-" + this.value, index);
          }
        }
      }
    },
    isInt(e) {
      let specialKeys = [];
      specialKeys.push(CODE_BACKSPACE);
      const keyCode = e.which ? e.which : e.keyCode;
      return (
        (keyCode >= CODE_0 && keyCode <= CODE_9) ||
        specialKeys.indexOf(keyCode) !== -1
      );
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

<docs>
   Example
```
<template>
  <div>
    <InputLong v-model="value" label="My long input label" description="Some help needed?"/>
    You typed: {{ JSON.stringify(value) }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: "123"
      };
    }
  };
</script>
```
   Example list
```
<template>
  <div>
    <InputLong v-model="value" :list="true" label="My long input label list" description="Some help needed?"/>
    You typed: {{ JSON.stringify(value) }}
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        value: ["123","-456"]
      };
    }
  };
</script>
```

</docs>
