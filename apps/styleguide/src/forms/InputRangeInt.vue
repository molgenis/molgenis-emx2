<template>
  <FormGroup v-bind="$props" class="input-group-range" v-on="$listeners">
    <InputAppend
      v-for="(item, idx) in valueArray"
      :key="idx + '.' + valueArray.length"
      v-bind="$props"
      :showClear="false"
      :showMinus="showMinus(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
      class="form-group"
    >
      <InputInt
        v-model="valueArray[idx][0]"
        @input="emitValue($event, idx, 0)"
        placeholder="from"
        :clear="false"
        style="margin: 0px"
      />
      <InputInt
        v-model="valueArray[idx][1]"
        @input="emitValue($event, idx, 1)"
        placeholder="to"
        :clear="false"
        style="margin: 0px"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import BaseInput from './_baseInput';
import InputInt from './InputInt';
import FormGroup from './_formGroup';
import InputAppend from './_inputAppend';

/** Input for integer values */
export default {
  components: {InputInt, FormGroup, InputAppend},
  extends: BaseInput,
  computed: {
    //@override
    valueArray() {
      let result = this.value;
      if (!Array.isArray(result)) {
        result = [];
        if (this.list) {
          result = [[]];
        }
      } else {
        //check each row
        if (!this.list) {
          result = [result];
        }
      }
      result = this.removeNulls(result);
      if (result.length == 0 || (this.list && this.showNewItem)) {
        result.push([null, null]);
      }
      return result;
    }
  },
  methods: {
    //@override
    removeNulls(arr) {
      return arr.filter((v) => v && (v[0] != null || v[1] != null));
    },
    //@override
    showPlus(idx) {
      //always on last line
      return (
        this.list &&
        !this.showNewItem &&
        idx == this.valueArray.length - 1 &&
        (this.valueArray[idx][0] != null || this.valueArray[idx][1] != null)
      );
    },
    //@override
    emitValue(event, idx, idx2) {
      this.showNewItem = false;
      let value = event ? (event.target ? event.target.value : event) : null;
      let result = this.valueArray;
      //update value
      if (idx >= result.length) result[idx] = [null, null];
      result[idx][idx2] = value;
      //remove nulls
      result = this.removeNulls(result);
      if (!this.list) {
        if (result && result.length > 0) result = result[0];
        else result = null;
      }
      this.$emit('input', result);
    }
  }
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
