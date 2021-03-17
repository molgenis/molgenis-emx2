<template>
    <div style="min-width: 10em">
        <InputString
            v-if="
                columnType.startsWith('STRING') ||
                    columnType.startsWith('TEXT') ||
                    columnType.startsWith('UUID')
            "
            :list="true"
            :or="true"
            :value="conditions"
            @input="$emit('update:conditions', $event)"
        />
        <InputRangeInt
            v-else-if="columnType.startsWith('INT')"
            :list="true"
            :value="conditions"
            @input="$emit('update:conditions', $event)"
        />
        <InputRangeDecimal
            v-else-if="columnType.startsWith('DECIMAL')"
            :list="true"
            :value="conditions"
            @input="$emit('update:conditions', $event)"
        />
        <InputRangeDate
            v-else-if="columnType.startsWith('DATE')"
            :list="true"
            :value="conditions"
            @input="$emit('update:conditions', $event)"
        />
        <InputCheckbox
            v-else-if="columnType.startsWith('BOOL')"
            :list="true"
            :options="['true', 'false']"
            :value="conditions"
            @input="$emit('update:conditions', $event)"
        />
        <InputRef
            v-else-if="columnType.startsWith('REF') || columnType == 'MREF'"
            :limit="7"
            :list="true"
            :table="refTable"
            :value="conditions"
            @input="$emit('update:conditions', $event)"
        />
        <div v-else>
            ERROR {{ column }}
        </div>
    </div>
</template>

<script>
import InputCheckbox from "../forms/InputCheckbox";
import InputString from "../forms/InputString";
import InputRangeInt from "../forms/InputRangeInt";
import InputRangeDecimal from "../forms/InputRangeDecimal";
import InputRangeDate from "../forms/InputRangeDate";
import InputRef from "../forms/InputRef";

export default {
  components: {
    InputCheckbox,
    InputString,
    InputRangeInt,
    InputRangeDecimal,
    InputRangeDate,
    InputRef,
  },
  props: {
    columnType: String,
    refTable: String,
    conditions: Array,
  },
};
</script>
