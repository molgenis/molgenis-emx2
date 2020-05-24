<template>
  <FormGroup v-bind="$props" class="input-group-range">
    <InputAppend
      v-for="(item, idx) in arrayValue"
      :key="item"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
      class="form-group"
    >
      <InputDecimal
        v-model="arrayValue[idx][0]"
        :defaultValue="arrayValue[idx][0]"
        placeholder="from"
        :clear="false"
        style="margin: 0px"
      />
      <InputDecimal
        v-model="arrayValue[idx][1]"
        :defaultValue="arrayValue[idx][1]"
        placeholder="to"
        :clear="false"
        style="margin: 0px"
      />
    </InputAppend>
  </FormGroup>
</template>

<script>
import InputRangeInt from './InputRangeInt'
import InputDecimal from './InputDecimal'
import FormGroup from './_formGroup'
import InputAppend from './_inputAppend'

/** Input for integer values */
export default {
  extends: InputRangeInt,
  components: { InputDecimal, FormGroup, InputAppend }
}
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <InputRangeDecimal :list="true" v-model="value" :defaultValue="value"/>
            {{value}}
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
