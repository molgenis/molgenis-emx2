<template>
  <div>
    <InputString
      v-if="columnType === 'STRING'"
      v-bind="$props"
      v-model="input"
    />
    <InputText
      v-else-if="columnType === 'TEXT'"
      v-bind="$props"
      v-model="input"
    />
    <InputInt
      v-else-if="columnType === 'INT'"
      v-bind="$props"
      v-model="input"
    />
    <InputDecimal
      v-else-if="columnType === 'DECIMAL'"
      v-bind="$props"
      v-model="input"
    />
    <InputBoolean
      v-else-if="columnType === 'BOOL'"
      v-bind="$props"
      v-model="input"
    />
    <InputRefSelect
      v-else-if="columnType === 'REF'"
      v-bind="$props"
      v-model="input"
      :table="table"
    />
    <InputDate
      v-else-if="columnType === 'DATE'"
      v-bind="$props"
      v-model="input"
    />
    <InputDateTime
      v-else-if="columnType === 'DATETIME'"
      v-bind="$props"
      v-model="input"
    />
    <InputRefSelect
      :list="true"
      v-else-if="
        columnType === 'REF_ARRAY' ||
        columnType === 'REFBACK' ||
        columnType === 'MREF'
      "
      v-bind="$props"
      v-model="input"
      :table="table"
      :graphqlURL="graphqlURL"
    />
    <InputString
      v-else-if="columnType === 'STRING_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="input"
    />
    <InputText
      v-else-if="columnType === 'TEXT_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="input"
    />
    <InputFile
      v-else-if="columnType === 'FILE'"
      v-bind="$props"
      v-model="input"
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
import InputDate from "../forms/InputDate";
import InputDateTime from "../forms/InputDateTime";
import InputFile from "../forms/InputFile";
import InputText from "../forms/InputText";

export default {
  name: "RowFormInput",
  extends: _baseInput,
  props: {
    schema: String,
    columnType: String,
    table: String,
    graphqlURL: {
      default: "graphql",
      type: String,
    },
  },
  data() {
    return {
      input: null,
    };
  },
  components: {
    InputString,
    InputInt,
    InputDecimal,
    InputBoolean,
    InputRefSelect: () => import("../forms/InputRefSelect"), //because it uses itself in nested form
    InputDate,
    InputDateTime,
    InputFile,
    InputText,
  },
  created() {
    this.input = this.value;
  },
  watch: {
    value() {
      this.input = this.value;
    },
    input() {
      this.$emit("input", this.input);
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
  <RowFormInput columnType="REF" label="Test ref" table="Pet" graphqlURL="/Pet store/graphql"/>
  <RowFormInput columnType="REF_ARRAY" label="Test ref" table="Pet" :defaultValue="[{name:'spike'}]"
                graphqlURL="/Pet store/graphql"/>
  <RowFormInput columnType="DATE" label="Test Date"/>
</div>
```
</docs>
