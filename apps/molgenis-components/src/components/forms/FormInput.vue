<template>
  <component :is="inplace ? 'InlineInput' : 'span'" :value="value">
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
import InputRefSelect from "../forms/InputRefSelect.vue"

const typeToInputMap = {
  HEADING: InputHeading,
  EMAIL: InputString,
  HYPERLINK: InputString,
  STRING: InputString,
  TEXT: InputText,
  INT: InputInt,
  LONG: InputLong,
  DECIMAL: InputDecimal,
  BOOL: InputBoolean,
  DATE: InputDate,
  REF: InputRefSelect,
  FILE: InputFile,
  DATETIME: InputDateTime,
  ONTOLOGY: InputOntology,
  STRING_ARRAY: ArrayInput,
  TEXT_ARRAY: ArrayInput,
  INT_ARRAY: ArrayInput,
  LONG_ARRAY: ArrayInput,
  DECIMAL_ARRAY: ArrayInput,
  BOOL_ARRAY: ArrayInput,
  DATE_ARRAY: ArrayInput,
  DATETIME_ARRAY: ArrayInput,
  ONTOLOGY_ARRAY: ArrayInput,
  REF_ARRAY: ArrayInput,
};

export default {
  name: "FormInput",
  extends: BaseInput,
  props: {
    inplace: {
      type: Boolean,
      default: false,
    },
    columnType: {
      type: String,
      required: true,
    },
    description: {
      type: String,
      default: null,
    },
    editMeta: {
      type: Boolean,
      required: false,
    },
    filter: {
      type: Object,
      required: false,
    },
    graphqlURL: {
      default: "graphql",
      type: String,
    },
    pkey: {
      type: String,
      required: false,
    },
    refBack: {
      type: String,
      required: false,
    },
    refBackType: {
      type: String,
      required: false,
    },
    refLabel: {
      type: String,
      required: false,
    },
    schema: {
      type: String,
      required: false,
    },
    tableName: {
      type: String,
      required: false,
    },
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
      <div>
        <FormInput
          id="string-example"
          columnType="STRING"
          label="Example string input"
          v-model="stringValue"
        />
      </div>
      <div>You typed: {{ stringValue }}</div>
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
      <div>
        <FormInput
          id="string-array-example"
          columnType="STRING_ARRAY"
          label="Example string array input"
          v-model="stringValueArray"
        />
      </div>
      <div>You typed: {{ JSON.stringify(stringValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="date-example"
          columnType="DATE"
          label="Example date input"
          v-model="dateValue"
        />
      </div>
      <div>You selected: {{ dateValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="date-array-example"
          columnType="DATE_ARRAY"
          label="Example date array input"
          v-model="dateValueArray"
        />
      </div>
      <div>You selected: {{ JSON.stringify(dateValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="date-time-example"
          columnType="DATETIME"
          label="Example date-time input"
          v-model="dateTimeValue"
        />
      </div>
      <div>You selected: {{ dateTimeValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="date-time-array-example"
          columnType="DATETIME_ARRAY"
          label="Example date-time array input"
          v-model="dateTimeValueArray"
        />
      </div>
      <div>You selected: {{ JSON.stringify(dateTimeValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="int-example"
          columnType="INT"
          label="Example integer input"
          v-model="intValue"
        />
      </div>
      <div>You typed: {{ intValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="int-array-example"
          columnType="INT_ARRAY"
          label="Example integer array input"
          v-model="intValueArray"
        />
      </div>
      <div>You typed: {{ JSON.stringify(intValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="text-example"
          columnType="TEXT"
          label="Example text input"
          v-model="textValue"
        />
      </div>
      <div>You typed: {{ textValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="text-array-example"
          columnType="TEXT_ARRAY"
          label="Example text array input"
          v-model="textValueArray"
        />
      </div>
      <div>You typed: {{ JSON.stringify(textValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="long-example"
          columnType="LONG"
          label="Example long input"
          v-model="longValue"
        />
      </div>
      <div>You typed: {{ longValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="long-array-example"
          columnType="LONG_ARRAY"
          label="Example long array input"
          v-model="longValueArray"
        />
      </div>
      <div>You typed: {{ JSON.stringify(longValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="decimal-example"
          columnType="DECIMAL"
          label="Example decimal input"
          v-model="decimalValue"
        />
      </div>
      <div>You typed: {{ decimalValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="decimal-array-example"
          columnType="DECIMAL_ARRAY"
          label="Example decimal array input"
          v-model="decimalValueArray"
        />
      </div>
      <div>You typed: {{ JSON.stringify(decimalValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="boolean-example"
          columnType="BOOL"
          label="Example boolean input"
          v-model="booleanValue"
        />
      </div>
      <div>You selected: {{ booleanValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="boolean-array-example"
          columnType="BOOL_ARRAY"
          label="Example boolean array input"
          v-model="booleanValueArray"
        />
      </div>
      <div>You selected: {{ JSON.stringify(booleanValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="ref-example"
          columnType="REF"
          label="Example ref input"
          tableName="Pet"
          :defaultValue="{ name: 'spike' }"
          :graphqlURL="graphqlUrl"
          v-model="refValue"
        />
      </div>
      <div>You selected: {{ JSON.stringify(refValue, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="ref-array-example"
          columnType="REF_ARRAY"
          label="Example ref array input"
          tableName="Pet"
          :defaultValue="[{ name: 'spike' }]"
          :graphqlURL="graphqlUrl"
          v-model="refValueArray"
        />
      </div>
      <div>You selected: {{ JSON.stringify(refValueArray, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="ontology-example"
          columnType="ONTOLOGY"
          label="Example ontology input"
          tableName="Category"
          v-model="ontologyValue"
          :graphqlURL="graphqlUrl"
        />
      </div>
      <div>You selected: {{ JSON.stringify(ontologyValue, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="ontology-array-example"
          columnType="ONTOLOGY_ARRAY"
          label="Example ontology array input"
          tableName="Category"
          v-model="ontologyArrayValue"
          :graphqlURL="graphqlUrl"
        />
      </div>
      <div>You selected: {{ JSON.stringify(ontologyArrayValue, null, 2) }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <FormInput
          id="file-input-example"
          columnType="FILE"
          label="Example file input"
          v-model="fileValue"
        />
      </div>
      <div>You selected: {{ fileValue }}</div>
    </DemoItem>
    <DemoItem>
      <div>
        <b>Example unsupported input</b>
      </div>
      <div>
        <FormInput
          id="unsupported-input-example"
          columnType="not_supported_input"
          inplace
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