<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(item, idx) in arrayValue"
      :key="item"
      v-bind="$props"
      :showClear="true"
      @clear="clearValue(idx)"
      :showPlus="false"
      @add="addRow"
      class="from-group"
    >
      <InputDate
        v-model="arrayValue[idx][0]"
        :defaultValue="arrayValue[idx][0]"
        placeholder="from"
        :clear="false"
        style="margin: 0px"
      />
      <InputDate
        v-model="arrayValue[idx][1]"
        :defaultValue="arrayValue[idx][1]"
        placeholder="to"
        :clear="false"
        style="margin: 0px"
      />
    </InputAppend>
    <div class="input-group">
      <AppendPlus v-if="showPlus(arrayValue.length - 1)" @add="addRow" />
    </div>
  </FormGroup>
</template>

<script>
import InputRangeInt from './InputRangeInt'
import InputDate from './InputDate'
import FormGroup from './_formGroup'
import InputAppend from './_inputAppend'

/** Input for integer values */
export default {
  extends: InputRangeInt,
  components: { InputDate, FormGroup, InputAppend }
}
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <InputRangeDate :list="true" v-model="value"/>
            {{value}}
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
    Example with default
    ```
    <template>
        <div>
            <InputRangeDate :list="true" v-model="value" :defaultValue="value"/>
            {{value}}
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
