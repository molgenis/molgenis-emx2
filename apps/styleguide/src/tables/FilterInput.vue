<template>
  <div style="min-width: 10em">
    <InputString
      :list="true"
      :or="true"
      v-if="
        column.columnType.startsWith('STRING') ||
        column.columnType.startsWith('TEXT') ||
        column.columnType.startsWith('UUID')
      "
      v-model="column.conditions"
      :defaultValue="column.conditions"
    />
    <InputRangeInt
      :list="true"
      v-if="column.columnType.startsWith('INT')"
      v-model="column.conditions"
      :defaultValue="column.conditions"
    />
    <InputRangeDecimal
      :list="true"
      v-if="column.columnType.startsWith('DECIMAL')"
      v-model="column.conditions"
      :defaultValue="column.conditions"
    />
    <InputRangeDate
      :list="true"
      v-if="column.columnType.startsWith('DATE')"
      v-model="column.conditions"
      :defaultValue="column.conditions"
    />
    <InputCheckbox
      :list="true"
      v-if="column.columnType.startsWith('BOOL')"
      :options="['true', 'false']"
      v-model="column.conditions"
      :defaultValue="column.conditions"
    />
    <InputRef
      :list="true"
      v-if="column.columnType.startsWith('REF') || column.columnType == 'MREF'"
      :refTable="column.refTable"
      v-model="column.conditions"
      :defaultValue="column.conditions"
      :limit="7"
    />
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
    column: Object,
  },
};
</script>
