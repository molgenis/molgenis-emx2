<template>
  <FormGroup v-bind="$props" class="input-group-range">
    <InputAppend
      v-for="(item, idx) in arrayValue"
      :key="idx + '.' + arrayValue.length"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showMinus="showMinus(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
      class="form-group"
    >
      <InputInt
        v-model="arrayValue[idx][0]"
        @input="emitValue"
        placeholder="from"
        :clear="false"
        style="margin: 0px"
      />
      <InputInt
        v-model="arrayValue[idx][1]"
        @input="emitValue"
        placeholder="to"
        :clear="false"
        style="margin: 0px"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import BaseInput from "./_baseInput";
import InputInt from "./InputInt";
import FormGroup from "./_formGroup";
import InputAppend from "./_inputAppend";

/** Input for integer values */
export default {
  components: { InputInt, FormGroup, InputAppend },
  extends: BaseInput,
  methods: {
    init() {
      if (this.value && Array.isArray(this.value) && this.value.length > 0) {
        if (this.list) {
          //deep copy
          this.arrayValue = JSON.parse(JSON.stringify(this.value));
        } else {
          //deep copy
          this.arrayValue = [JSON.parse(JSON.stringify(this.value))];
        }
      } else {
        this.arrayValue = [[null, null]];
      }
    },
    addRow() {
      this.arrayValue.push([null, null]);
    },
    clearValue(idx) {
      if (this.arrayValue.length > 1) {
        this.arrayValue.splice(idx, 1);
      } else {
        this.arrayValue = [[null, null]];
      }
      this.emitValue();
    },
    //override from baseinput
    emitValue() {
      let result;
      if (!Array.isArray(this.arrayValue)) {
        result = [null, null];
      } else {
        result = this.arrayValue;
        result = result.filter((e) => e[0] || e[1]);
      }
      if (!this.list) {
        result = result[0];
      }
      this.$emit("input", result);
    },
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputRangeInt v-model="value"/>
    {{ JSON.stringify(value) }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: null
      }
    }
  }
</script>
```
Example with default
```
<template>
  <div>
    <InputRangeInt v-model="value"/>
    {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: [1, 2]
      }
    }
  }
</script>
```
Example list
```
<template>
  <div>
    <InputRangeInt :list="true" v-model="value"/>
    {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: null
      }
    }
  }
</script>
```
Example with list and default
```
<template>
  <div>
    <InputRangeInt :list="true" v-model="value"/>
    {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: [[1, 2], [3, 4]]
      }
    }
  }
</script>
```
</docs>
