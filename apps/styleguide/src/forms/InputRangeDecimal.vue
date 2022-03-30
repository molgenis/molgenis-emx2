<template>
  <FormGroup v-bind="$props" class="input-group-range" v-on="$listeners">
    <InputAppend
      v-for="(item, idx) in valueArray"
      :key="idx"
      v-bind="$props"
      :showClear="false"
      :showPlus="showPlus(idx)"
      @add="addRow"
      class="form-group"
    >
      <InputDecimal
        v-model="valueArray[idx][0]"
        placeholder="from"
        :clear="false"
        style="margin: 0px"
        @input="emitValue($event, idx, 0)"
      />
      <InputDecimal
        v-model="valueArray[idx][1]"
        placeholder="to"
        :clear="false"
        style="margin: 0px"
        @input="emitValue($event, idx, 1)"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import InputRangeInt from './InputRangeInt';
import InputDecimal from './InputDecimal';
import FormGroup from './_formGroup';
import InputAppend from './_inputAppend';

/** Input for integer values */
export default {
  extends: InputRangeInt,
  components: {InputDecimal, FormGroup, InputAppend}
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputRangeDecimal v-model="value"/>
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
Example with default
```
<template>
  <div>
    <InputRangeDecimal v-model="value"/>
    {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: [1.0, 2.0]
      }
    }
  }
</script>
```
Example list
```
<template>
  <div>
    <InputRangeDecimal :list="true" v-model="value"/>
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
Example list and default
```
<template>
  <div>
    <InputRangeDecimal :list="true" v-model="value"/>
    {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: [[1.0, 2.0], [3.1, 4.5]]
      }
    }
  }
</script>
```
</docs>
