<template>
  <div>
    <InputString
      v-if="columnType === 'STRING'"
      v-bind="$props"
      v-model="value"
    />
    <InputText
      v-else-if="columnType === 'TEXT'"
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
    <InputRefSelect
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
    <InputRefSelect
      :list="true"
      v-else-if="
        columnType === 'REF_ARRAY' ||
        columnType === 'REFBACK' ||
        columnType === 'MREF'
      "
      v-bind="$props"
      v-model="value"
      :graphqlURL="graphqlURL"
    />
    <InputString
      v-else-if="columnType === 'STRING_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="value"
    />
    <InputText
      v-else-if="columnType === 'TEXT_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="value"
    />
    <InputFile
      v-else-if="columnType === 'FILE'"
      v-bind="$props"
      v-model="value"
    />
    <div v-else>UNSUPPORTED TYPE '{{ columnType }}'</div>
  </div>
</template>

<script>
import _baseInput from "../forms/_baseInput";
import InputString from "../forms/InputString";
import InputInt from "../forms/InputInt";
import InputDecimal from "../forms/InputDecimal";
import InputBoolean from "../forms/InputBoolean";
import InputRefSelect from "../forms/InputRefSelect";
import InputDate from "../forms/InputDate";
import InputDateTime from "../forms/InputDateTime";
import InputFile from "../forms/InputFile";
import InputText from "../forms/InputText";

export default {
  extends: _baseInput,
  props: {
    schema: String,
    columnType: String,
    refTable: String,
    defaultValue: [String, Number, Object, Array],
    graphqlURL: {
      default: "graphql",
      type: String,
    },
  },
  components: {
    InputString,
    InputInt,
    InputDecimal,
    InputBoolean,
    InputRefSelect,
    InputDate,
    InputDateTime,
    InputFile,
    InputText,
  },
  watch: {
    value() {
      this.$emit("input", this.value);
    },
  },
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
