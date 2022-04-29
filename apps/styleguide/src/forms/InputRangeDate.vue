<template>
  <FormGroup v-bind="$props" class="input-group-range" v-on="$listeners">
    <InputAppend
      v-for="(item, idx) in valueArray"
      :key="idx"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
      class="form-group"
    >
      <InputDate
        v-model="valueArray[idx][0]"
        placeholder="from"
        :clear="false"
        style="margin: 0px"
        @input="emitValue($event, idx, 0)"
      />
      <InputDate
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
import InputDate from './InputDate';
import FormGroup from './_formGroup';
import InputAppend from './_inputAppend';

/** Input for integer values */
export default {
  extends: InputRangeInt,
  components: {InputDate, FormGroup, InputAppend}
};
</script>

<docs>
Example
```
<template>
  <div>
    <InputRangeDate v-model="value"/>
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
Example with list
```
<template>
  <div>
    <InputRangeDate :list="true" v-model="value"
    />
    {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: []
      }
    }
  }
</script>
```
Example with list and default
```
<template>
  <div>
    <InputRangeDate :list="true" v-model="value"/>
    {{ value }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        value: [['2020-1-1', '2020-2-2'], ['1999-10-10', null]]
      }
    }
  }
</script>
```
</docs>
