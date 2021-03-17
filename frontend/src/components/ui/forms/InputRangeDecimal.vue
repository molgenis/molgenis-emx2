<template>
    <FormGroup v-bind="$props" class="input-group-range">
        <InputAppend
            v-for="(item, idx) in valueArray"
            :key="idx"
            v-bind="$props"
            class="form-group"
            :show-clear="showClear(idx)"
            :show-plus="showPlus(idx)"
            @add="addRow"
            @clear="clearValue(idx)"
        >
            <InputDecimal
                v-model="valueArray[idx][0]"
                :clear="false"
                placeholder="from"
                style="margin: 0px"
                @input="emitValue($event, idx, 0)"
            />
            <InputDecimal
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
import InputRangeInt from "./InputRangeInt.vue";
import InputDecimal from "./InputDecimal.vue";
import FormGroup from "./_formGroup.vue";
import InputAppend from "./_inputAppend.vue";

/** Input for integer values */
export default {
  components: { InputDecimal, FormGroup, InputAppend },
  extends: InputRangeInt,
};
</script>
