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
    InputOntology: () => import("../forms/InputOntology.vue"), //because it uses itself in nested form,
    InputRef: () => import("../forms/InputRef.vue"), //because it uses itself in nested form,
    //  InputRefback: () => import("../forms/InputRefback.vue"), //because it uses itself in nested form,
    //  InputRefSelect: () => import("../forms/InputRefSelect.vue"), //because it uses itself in nested form
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
        id="ontology"
        columnType="ONTOLOGY"
        label="Test ontology"
        ontologyTableName="Category"
        v-model="valueOntology"
        graphqlURL="/pet store/graphql"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="heading"
        columnType="HEADING"
        label="my header"
        description="my description"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="string"
        columnType="STRING"
        label="Test String"
        v-model="value"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="string-inplace"
        columnType="STRING"
        label="Test String inplace"
        v-model="valueInplace"
        inplace
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="string-array"
        columnType="STRING_ARRAY"
        label="Test String Array"
        v-model="valueArray"
      />
    </DemoItem>
    <DemoItem>
      <FormInput
        id="ref-array"
        columnType="REF_ARRAY"
        label="Test ref"
        table="Pet"
        :defaultValue="[{ name: 'spike' }]"
        graphqlURL="/pet store/graphql"
      />
    </DemoItem>
    <DemoItem>
      <FormInput id="date" columnType="DATE" label="Test Date" />
    </DemoItem>
  </div>
</template>
<script>
export default {
  data: function () {
    return {
      value: "test",
      valueInplace: "inplace",
      valueArray: ["value1", "value2"],
      valueOntology: null
    };
  },
};
</script>
</docs>
