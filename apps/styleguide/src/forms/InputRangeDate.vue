<template>
  <FormGroup v-bind="$props" class="input-group-range">
    <InputAppend
      v-for="(item, idx) in arrayValue"
      :key="idx"
      v-bind="$props"
      :showClear="showClear(idx)"
      @clear="clearValue(idx)"
      :showPlus="showPlus(idx)"
      @add="addRow"
      class="form-group"
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
  </FormGroup>
</template>

<script>
import InputRangeInt from "./InputRangeInt";
import InputDate from "./InputDate";
import FormGroup from "./_formGroup";
import InputAppend from "./_inputAppend";

/** Input for integer values */
export default {
  extends: InputRangeInt,
  components: { InputDate, FormGroup, InputAppend }
};
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <InputRangeDate v-model="value"/>
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
    Example with list
    ```
    <template>
        <div>
            <InputRangeDate :list="true" v-model="value"
            />
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
    Example with list and default
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
