<template>
  <div>
    <InputHeading
      v-if="columnType === 'HEADING'"
      v-bind="$props"
      v-on="$listeners"
    />
    <InputString
      v-else-if="columnType === 'STRING'"
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
      v-else-if="columnType.startsWith('ONTOLOGY')"
      v-bind="$props"
      v-model="input"
      :table="table"
      :list="columnType.includes('ARRAY')"
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
    <inputRefback
      v-else-if="refBackType == 'REF'"
      v-bind="$props"
      :table="table"
    />
    <InputString
      v-else-if="columnType === 'STRING_ARRAY'"
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
import InputDecimal from "../forms/InputDecimal";
import InputBoolean from "../forms/InputBoolean";
import InputDate from "../forms/InputDate";
import InputDateTime from "../forms/InputDateTime";
import InputFile from "../forms/InputFile";
import InputText from "../forms/InputText";
import InputHeading from "../forms/InputHeading";
import InputOntology from "../forms/InputOntology";

export default {
  name: "RowFormInput",
  extends: _baseInput,
  props: {
    /** enable editing of label and description*/
    editMeta: Boolean,
    schema: String,
    columnType: String,
    description: String,
    filter: Object,
    table: String,
    refLabel: String,
    refBack: String,
    refBackType: String,
    pkey: Object,
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
    InputHeading,
    InputOntology,
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
  <RowFormInput columnType="REF" label="Test ref" table="Pet" graphqlURL="/Pet store/graphql"/>
  <RowFormInput columnType="REF_ARRAY" label="Test ref" table="Pet" :defaultValue="[{name:'spike'}]"
                graphqlURL="/Pet store/graphql"/>
  <RowFormInput columnType="DATE" label="Test Date"/>
  <RowFormInput columnType="ONTOLOGY_ARRAY" label="Test ontology" table="AreasOfInformation"
                graphqlURL="/CohortNetwork/graphql"/>
</div>
```

</docs>
