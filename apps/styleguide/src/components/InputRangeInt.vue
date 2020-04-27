<template>
  <FormGroup v-bind="$props">
    <InputAppend
      v-for="(item, idx) in arrayValue"
      :key="item"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showPlus="false"
      @add="addRow"
      class="form-group"
    >
      <InputInt
        v-model="arrayValue[idx][0]"
        :defaultValue="arrayValue[idx][0]"
        placeholder="from"
        :clear="false"
        style="margin: 0px"
      />
      <InputInt
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
import BaseInput from './_baseInput'
import InputInt from './InputInt'
import FormGroup from './_formGroup'
import InputAppend from './_inputAppend'
import AppendPlus from './_appendPlus'

/** Input for integer values */
export default {
  extends: BaseInput,
  methods: {
    showPlus(idx) {
      return (
        this.list && idx === this.arrayValue.length - 1 && this.showClear(idx)
      )
    },
    showClear(idx) {
      if (this.arrayValue[idx] == undefined) {
        this.arrayValue = [[null, null]]
      }
      return (
        this.arrayValue[idx][0] !== null || this.arrayValue[idx][1] !== null
      )
    },
    addRow() {
      this.arrayValue.push([null, null])
    },
    clearValue(idx) {
      if (this.arrayValue.length > 1) {
        this.arrayValue.splice(idx, 1)
      } else {
        this.arrayValue = [[null, null]]
      }
    }
  },
  components: { InputInt, FormGroup, InputAppend, AppendPlus }
}
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <InputRangeInt :list="true" v-model="value"/>
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
            <InputRangeInt :list="true" v-model="value" :defaultValue="value"/>
            {{value}}
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
