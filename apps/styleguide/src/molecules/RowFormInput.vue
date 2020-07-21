<template>
  <div>
    <InputString
      v-if="columnType === 'STRING'"
      v-bind="$props"
      v-model="value"
    />
    <InputInt
      v-else-if="columnType === 'INT'"
      v-bind="$props"
      v-model="value"
    />
    <InputDecimal
      v-else-if="columnType === 'DECIMAL'"
      v-bind="$props"
      v-model="value"
    />
    <InputBoolean
      v-else-if="columnType === 'BOOL'"
      v-bind="$props"
      v-model="value"
    />
    <InputRef
      v-else-if="columnType === 'REF'"
      v-bind="$props"
      v-model="value"
    />
    <InputDate
      v-else-if="columnType === 'DATE'"
      v-bind="$props"
      v-model="value"
    />
    <InputDateTime
      v-else-if="columnType === 'DATETIME'"
      v-bind="$props"
      v-model="value"
    />
    <InputRef
      :list="true"
      v-else-if="
        columnType === 'REF_ARRAY' ||
          columnType === 'REFBACK' ||
          columnType === 'MREF'
      "
      v-bind="$props"
      v-model="value"
    />
    <InputString
      v-else-if="columnType === 'STRING_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="value"
    />
    <div v-else>UNSUPPORTED TYPE {{ columnType }}</div>
  </div>
</template>

<script>
import _baseInput from "../components/_baseInput";
import InputString from "../components/InputString";
import InputInt from "../components/InputInt";
import InputDecimal from "../components/InputDecimal";
import InputBoolean from "../components/InputBoolean";
import InputRef from "../components/InputRef";
import InputDate from "../components/InputDate";
import InputDateTime from "../components/InputDateTime";

export default {
  extends: _baseInput,
  props: {
    schema: String,
    columnType: String,
    refTable: String,
    refColumn: String,
    defaultValue: [String, Number, Object, Array]
  },
  components: {
    InputString,
    InputInt,
    InputDecimal,
    InputBoolean,
    InputRef,
    InputDate,
    InputDateTime
  },
  watch: {
    value() {
      this.$emit("input", this.value);
    }
  }
};
</script>

<docs>
    Example:
    ```
    <div>
        <RowFormInput columnType="STRING" label="Test String"/>
        <RowFormInput columnType="STRING_ARRAY" label="Test String"/>
        <RowFormInput columnType="REF" label="Test ref" refTable="Pet"/>
        <RowFormInput columnType="REF_ARRAY" label="Test ref" refTable="Pet" :defaultValue="[{name:'spike'}]"/>
        <RowFormInput columnType="DATE" label="Test Date"/>
    </div>
    ```
</docs>
