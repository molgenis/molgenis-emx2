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
import { CODE_MINUS } from "../constants";
import { isNumericKey } from "./utils/InputUtils";

export default {
  extends: BaseInput,
  components: {
    InputAppend,
    FormGroup: () => import("./_formGroup"), //because it uses itself in nested form
    IconAction,
  },
  methods: {
    keyhandler(event, index) {
      const keyCode = event.which ? event.which : event.keyCode;
      if (keyCode === CODE_MINUS) this.flipSign(index);
      if (!isNumericKey(event)) event.preventDefault();
    },
    flipSign(index) {
      if (this.value && this.value.length > 0) {
        const value = Array.isArray(this.value)
          ? this.value[index]
          : this.value;
        if (value.charAt(0) === "-") {
          this.emitValue(value.substring(1), index);
        } else {
          this.emitValue("-" + value, index);
        }
      }
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
Example long input
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
        value: "9223372036854775807"
      };
    }
  };
</script>
```
Example long list
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
        value: ["9223372036854775807","-9223372036854775807"]
      };
    }
  };
</script>
```

</docs>
