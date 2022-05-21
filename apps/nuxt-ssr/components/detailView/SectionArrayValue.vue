<template>
  <div>
    <component
      v-for="(value, index) in data"
      :key="index"
      :is="fieldTypeComponentName"
      :data="value"
      :metaData="metaData"
      :color="color"
    />
  </div>
</template>

<script>
import OntologyFieldValue from "./OntologyFieldValue.vue";
import { StringDisplay, TextDisplay, ObjectDisplay } from "molgenis-components";

export default {
  name: "SectionArrayValue",
  components: {
    StringDisplay,
    TextDisplay,
    ObjectDisplay,
    OntologyFieldValue,
  },
  props: {
    data: {
      type: [Array],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
    color: {
      type: String,
      default: () => "primary",
    },
  },
  computed: {
    fieldTypeComponentName() {
      return {
        BOOL_ARRAY: "StringDisplay",
        UUID_ARRAY: "StringDisplay",
        STRING_ARRAY: "StringDisplay",
        TEXT_ARRAY: "TextDisplay",
        INT_ARRAY: "StringDisplay",
        DECIMAL_ARRAY: "StringDisplay",
        DATE_ARRAY: "StringDisplay",
        DATETIME: "StringDisplay",
        DATETIME_ARRAY: "StringDisplay",
        JSONB_ARRAY: "ObjectDisplay",
        ONTOLOGY_ARRAY: "OntologyFieldValue",
      }[this.metaData.columnType];
    },
  },
};
</script>
