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
  </FormGroup>
</template>

<script>
    import BaseInput from "./_baseInput";
    import InputInt from "./InputInt";
    import FormGroup from "./_formGroup";
    import InputAppend from "./_inputAppend";

    /** Input for integer values */
export default {
  extends: BaseInput,
  methods: {
    showPlus(idx) {
      if (this.arrayValue[idx] == undefined) {
        this.arrayValue = [[null, null]];
      }
      return (
        this.list &&
        idx === this.arrayValue.length - 1 &&
        (this.arrayValue[idx][0] !== null || this.arrayValue[idx][1] !== null)
      );
    },
    showClear(idx) {
      if (this.arrayValue[idx] == undefined) {
        this.arrayValue = [[null, null]];
      }
      return true;
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
    },
    //override from baseinput
    emitValue() {
      if (this.list) {
        //replace empty strings to null
        this.value = Array.isArray(this.arrayValue)
          ? this.arrayValue.map(v =>
              Array.isArray(v)
                ? v.map(v2 =>
                    !v2 || v2.length === 0 || !String(v2).trim() ? null : v2
                  )
                : null
            )
          : null;
        //filter [null,null] also
        this.value = Array.isArray(this.value)
          ? this.value.filter(
              el => Array.isArray(el) && el.some(v => v != null)
            )
          : null;
      } else {
        this.value = Array.isArray(this.arrayValue[0])
          ? this.arrayValue[0].map(v =>
              !v || v.length === 0 || !v.trim() ? null : v
            )
          : null;
      }
      this.$emit("input", this.value);
    }
  },
  components: { InputInt, FormGroup, InputAppend }
};
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
