<template>
  <div>
    <InputHeading
      v-if="columnType === 'HEADING'"
      v-bind="$props"
      v-on="$listeners"
    />
    <InputString
      v-else-if="
        columnType === 'STRING' ||
        columnType === 'EMAIL' ||
        columnType === 'HYPERLINK'
      "
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputText
      v-else-if="columnType === 'TEXT'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputLong
      v-else-if="columnType === 'LONG'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputInt
      v-else-if="columnType === 'INT'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputDecimal
      v-else-if="columnType === 'DECIMAL'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputBoolean
      v-else-if="columnType === 'BOOL'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputOntology
      v-else-if="columnType === 'ONTOLOGY' || columnType === 'ONTOLOGY_ARRAY'"
      v-bind="$props"
      v-model="input"
      :table="table"
      :list="columnType === 'ONTOLOGY_ARRAY'"
      v-on="$listeners"
    />
    <InputRefSelect
      v-else-if="columnType === 'REF'"
      v-bind="$props"
      v-model="input"
      :filter="filter"
      :table="table"
      v-on="$listeners"
    />
    <InputDate
      v-else-if="columnType === 'DATE'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputDateTime
      v-else-if="columnType === 'DATETIME'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputRef
      :list="true"
      :multiple-columns="true"
      :max-num="35"
      v-else-if="
        columnType === 'REF_ARRAY' ||
        columnType === 'MREF' ||
        refBackType === 'REF_ARRAY'
      "
      v-bind="$props"
      v-model="input"
      :table="table"
      :graphqlURL="graphqlURL"
      v-on="$listeners"
    />
    <InputRefback
      v-else-if="refBackType == 'REF'"
      v-bind="$props"
      :table="table"
    />
    <InputString
      v-else-if="
        columnType === 'STRING_ARRAY' ||
        columnType === 'EMAIL_ARRAY' ||
        columnType === 'HYPERLINK_ARRAY'
      "
      :list="true"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputText
      v-else-if="columnType === 'TEXT_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputLong
      v-else-if="columnType === 'LONG_ARRAY'"
      :list="true"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <InputFile
      v-else-if="columnType === 'FILE'"
      v-bind="$props"
      v-model="input"
      v-on="$listeners"
    />
    <div v-else>UNSUPPORTED TYPE '{{ columnType }}'</div>
  </div>
</template>

<script>
import _baseInput from "../forms/_baseInput";
import InputString from "../forms/InputString";
import InputInt from "../forms/InputInt";
import InputLong from "../forms/InputLong";
import InputDecimal from "../forms/InputDecimal";
import InputBoolean from "../forms/InputBoolean";
import InputDate from "../forms/InputDate";
import InputDateTime from "../forms/InputDateTime";
import InputFile from "../forms/InputFile";
import InputText from "../forms/InputText";
import InputHeading from "molgenis-components/src/components/forms/InputHeading";

export default {
  name: "RowFormInput",
  extends: _baseInput,
  props: {
    /** enable editing of label and description*/
    columnType: String,
    description: String,
    editMeta: Boolean,
    filter: Object,
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    pkey: Object,
    refBack: String,
    refBackType: String,
    refLabel: String,
    schema: String,
    table: String,
  },
  data() {
    return {
      input: null,
    };
  },
  components: {
    InputString,
    InputInt,
    InputLong,
    InputDecimal,
    InputBoolean,
    InputDate,
    InputDateTime,
    InputFile,
    InputText,
    InputHeading,
    InputRefSelect: () => import("../forms/InputRefSelect"), //because it uses itself in nested form
    InputOntology: () => import("../forms/InputOntology"), //because it uses itself in nested form,
    InputRef: () => import("../forms/InputRef"), //because it uses itself in nested form,
    InputRefback: () => import("../forms/InputRefback"), //because it uses itself in nested form,
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
  <RowFormInput columnType="HEADING" label="my header" description="my description"/>
  <RowFormInput columnType="STRING" label="Test String"/>
  <RowFormInput columnType="STRING_ARRAY" label="Test String"/>
  <RowFormInput columnType="REF" label="Test ref" table="Pet" graphqlURL="/pet store/graphql"/>
  <RowFormInput columnType="REF_ARRAY" label="Test ref" table="Pet" :defaultValue="[{name:'spike'}]"
                graphqlURL="/pet store/graphql"/>
  <RowFormInput columnType="DATE" label="Test Date"/>
  <RowFormInput columnType="ONTOLOGY_ARRAY" label="Test ontology" table="Category"
                graphqlURL="/pet store/graphql"/>
</div>
```

</docs>
