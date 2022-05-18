<template>
  <component :is="inplace ? 'InlineInput' : 'div'" :value="value">
    <component
      v-if="typeToInput"
      :is="typeToInput"
      v-bind="$props"
      v-on="$listeners"
    />
    <div v-else>UNSUPPORTED TYPE '{{ columnType }}'</div>
  </component>
</template>

<script>
import BaseInput from "../forms/baseInputs/BaseInput.vue";
import InlineInput from "../forms/InlineInput.vue";
import ArrayInput from "../forms/ArrayInput.vue";
import InputString from "../forms/InputString.vue";
import InputInt from "../forms/InputInt.vue";
import InputLong from "../forms/InputLong.vue";
import InputDecimal from "../forms/InputDecimal.vue";
import InputBoolean from "../forms/InputBoolean.vue";
import InputDate from "../forms/InputDate.vue";
import InputDateTime from "../forms/InputDateTime.vue";
import InputFile from "../forms/InputFile.vue";
import InputText from "../forms/InputText.vue";
import InputHeading from "../forms/InputHeading.vue";
import InputOntology from "../forms/InputOntology.vue";
import InputRef from "../forms/InputRef.vue";

const typeToInputMap = {
  HEADING: InputHeading,
  STRING: InputString,
  STRING_ARRAY: ArrayInput,
  TEXT: InputText,
  TEXT_ARRAY: ArrayInput,
  INT: InputInt,
  INT_ARRAY: ArrayInput,
  LONG: InputLong,
  LONG_ARRAY: ArrayInput,
  DECIMAL: InputDecimal,
  DECIMAL_ARRAY: ArrayInput,
  BOOL: InputBoolean,
  BOOL_ARRAY: ArrayInput,
  DATE: InputDate,
  DATE_ARRAY: ArrayInput,
  DATETIME: InputDateTime,
  DATETIME_ARRAY: ArrayInput,
  ONTOLOGY: InputOntology,
  ONTOLOGY_ARRAY: ArrayInput,
  REF: InputRef,
  REF_ARRAY: ArrayInput,
  FILE: InputFile,
};

export default {
  name: "FormInput",
  extends: BaseInput,
  props: {
    inplace: {
      type: Boolean,
    },
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
    tableName: String,
    ontologyTableName: String,
  },
  components: {
    InlineInput,
    ArrayInput,
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
    InputOntology,
    InputRef,
    //  InputRefback
    //  InputRefSelect
  },
  computed: {
    typeToInput() {
      return typeToInputMap[this.columnType];
    },
  },
};
</script>

<docs>
<template>
  <div>
    <DemoItem>
      <FormInput
        id="heading-example"
        columnType="HEADING"
        label="Example header"
        description="Header description"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="string-example"
        columnType="STRING"
        label="Example string input"
        v-model="stringValue"
      />
    </DemoItem>
    <DemoItem>
      <div><b>In place string example</b></div>
      <div>
        This is inside this
        <FormInput
          id="string-inplace-example"
          columnType="STRING"
          label="Example string input inplace"
          v-model="stringValueInplace"
          inplace
        />
        sentence
      </div>
    </DemoItem>
    <DemoItem>
      <FormInput
        id="string-array-example"
        columnType="STRING_ARRAY"
        label="Example string array input"
        v-model="stringValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="date-example"
        columnType="DATE"
        label="Example date input"
        v-model="dateValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="date-array-example"
        columnType="DATE_ARRAY"
        label="Example date array input"
        v-model="dateValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="date-time-example"
        columnType="DATETIME"
        label="Example date-time input"
        v-model="dateTimeValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="date-time-array-example"
        columnType="DATETIME_ARRAY"
        label="Example date-time array input"
        v-model="dateTimeValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="int-example"
        columnType="INT"
        label="Example integer input"
        v-model="intValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="int-array-example"
        columnType="INT_ARRAY"
        label="Example integer array input"
        v-model="intValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="text-example"
        columnType="TEXT"
        label="Example text input"
        v-model="textValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="text-array-example"
        columnType="TEXT_ARRAY"
        label="Example text array input"
        v-model="textValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="long-example"
        columnType="LONG"
        label="Example long input"
        v-model="longValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="long-array-example"
        columnType="LONG_ARRAY"
        label="Example long array input"
        v-model="longValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="decimal-example"
        columnType="DECIMAL"
        label="Example decimal input"
        v-model="decimalValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="decimal-array-example"
        columnType="DECIMAL_ARRAY"
        label="Example decimal array input"
        v-model="decimalValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="boolean-example"
        columnType="BOOL"
        label="Example boolean input"
        v-model="booleanValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="boolean-array-example"
        columnType="BOOL_ARRAY"
        label="Example boolean array input"
        v-model="booleanValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="ref-example"
        columnType="REF"
        label="Example ref input"
        tableName="Pet"
        :defaultValue="{ name: 'spike' }"
        :graphqlURL="graphqlUrl"
        v-model="refValue"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="ref-array-example"
        columnType="REF_ARRAY"
        label="Example ref array input"
        tableName="Pet"
        :defaultValue="[{ name: 'spike' }]"
        :graphqlURL="graphqlUrl"
        v-model="refValueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="ontology-example"
        columnType="ONTOLOGY"
        label="Example ontology input"
        ontologyTableName="Category"
        v-model="ontologyValue"
        :graphqlURL="graphqlUrl"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="ontology-array-example"
        columnType="ONTOLOGY_ARRAY"
        label="Example ontology array input"
        ontologyTableName="Category"
        v-model="ontologyArrayValue"
        :graphqlURL="graphqlUrl"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="file-input-example"
        columnType="FILE"
        label="Example file input"
        v-model="fileValue"
      />
    </DemoItem>
    <DemoItem>
      <div>
        <b>Example unsupported input</b>
      </div>
      <div>
        <FormInput
          id="unsupported-input-example"
          columnType="not_supported_input"
        />
      </div>
    </DemoItem>
  </div>
</template>
<script>
const graphqlUrl = "/pet store/graphql";
export default {
  data: function () {
    return {
      graphqlUrl,
      stringValue: "test",
      stringValueInplace: "inplace",
      stringValueArray: ["value1", "value2"],
      ontologyValue: null,
      ontologyArrayValue: [],
      dateValue: null,
      dateValueArray: [null, null],
      dateTimeValue: null,
      dateTimeValueArray: [null, null],
      intValue: 42,
      intValueArray: [5, 37],
      textValue: "example text",
      textValueArray: ["text", "more text"],
      longValue: "1337",
      longValueArray: ["0", "1.1"],
      decimalValue: 3.7,
      decimalValueArray: [4.2, 13.37],
      booleanValue: true,
      booleanValueArray: [true, false],
      refValue: null,
      refValueArray: [null, null],
      fileValue: null,
    };
  },
};
</script>
</docs>
