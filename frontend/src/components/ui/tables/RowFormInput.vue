<template>
    <div>
        <InputString
            v-if="columnType === 'STRING'"
            v-model="input"
            v-bind="$props"
        />
        <InputText
            v-else-if="columnType === 'TEXT'"
            v-model="input"
            v-bind="$props"
        />
        <InputInt
            v-else-if="columnType === 'INT'"
            v-model="input"
            v-bind="$props"
        />
        <InputDecimal
            v-else-if="columnType === 'DECIMAL'"
            v-model="input"
            v-bind="$props"
        />
        <InputBoolean
            v-else-if="columnType === 'BOOL'"
            v-model="input"
            v-bind="$props"
        />
        <InputRefSelect
            v-else-if="columnType === 'REF'"
            v-model="input"
            v-bind="$props"
            :table="table"
        />
        <InputDate
            v-else-if="columnType === 'DATE'"
            v-model="input"
            v-bind="$props"
        />
        <InputDateTime
            v-else-if="columnType === 'DATETIME'"
            v-model="input"
            v-bind="$props"
        />
        <InputRefSelect
            v-else-if="
                columnType === 'REF_ARRAY' ||
                    columnType === 'REFBACK' ||
                    columnType === 'MREF'
            "
            v-model="input"
            v-bind="$props"
            :graphql-u-r-l="graphqlURL"
            :list="true"
            :table="table"
        />
        <InputString
            v-else-if="columnType === 'STRING_ARRAY'"
            v-model="input"
            :list="true"
            v-bind="$props"
        />
        <InputText
            v-else-if="columnType === 'TEXT_ARRAY'"
            v-model="input"
            :list="true"
            v-bind="$props"
        />
        <InputFile
            v-else-if="columnType === 'FILE'"
            v-model="input"
            v-bind="$props"
        />
        <div v-else>
            UNSUPPORTED TYPE '{{ columnType }}'
        </div>
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
  watch: {
    value() {
      this.input = this.value;
    },
    input() {
      this.$emit("input", this.input);
    },
  },
  created() {
    this.input = this.value;
  },
};
</script>
