<template>
    <FormGroup v-bind="$props" class="input-group-range">
        <InputAppend
            v-for="(item, idx) in valueArray"
            :key="idx + '.' + valueArray.length"
            v-bind="$props"
            class="form-group"
            :show-clear="showClear(idx)"
            :show-minus="showMinus(idx)"
            :show-plus="showPlus(idx)"
            @add="addRow"
            @clear="clearValue(idx)"
        >
            <InputInt
                v-model="valueArray[idx][0]"
                :clear="false"
                placeholder="from"
                style="margin: 0px"
                @input="emitValue($event, idx, 0)"
            />
            <InputInt
                v-model="valueArray[idx][1]"
                :clear="false"
                placeholder="to"
                style="margin: 0px"
                @input="emitValue($event, idx, 1)"
            />
        </InputAppend>
    </FormGroup>
</template>

<script>
import BaseInput from "./_baseInput.vue";
import InputInt from "./InputInt.vue";
import FormGroup from "./_formGroup.vue";
import InputAppend from "./_inputAppend.vue";

/** Input for integer values */
export default {
  components: { InputInt, FormGroup, InputAppend },
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
    },
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
      this.$emit("input", result);
    },
  },
};
</script>
