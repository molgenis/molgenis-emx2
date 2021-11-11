<template>
  <div class="m-2 showcontainer row">
    <sectionFieldLabel
      class="col-4"
      :label="field.meta.name"
      :tooltip="field.description"
      :color="color"
    ></sectionFieldLabel>

    <div class="col-8">
      <component
        v-if="!isEmptyValue"
        :is="cellTypeComponentName"
        :data="field.value"
        :metaData="field.meta"
      />
      <div v-else>N/A</div>
    </div>
  </div>
</template>

<script>
import SectionFieldLabel from "./SectionFieldLabel.vue";
import RefBackFieldValue from "./RefBackFieldValue.vue";
import RefArrayFieldValue from "./RefArrayFieldValue.vue";
import {
  StringDisplay,
  TextDisplay,
  FileDisplay,
  ObjectDisplay,
} from "@mswertz/emx2-styleguide";

export default {
  name: "SectionField",
  components: {
    SectionFieldLabel,
    StringDisplay,
    TextDisplay,
    FileDisplay,
    ObjectDisplay,
    RefBackFieldValue,
    RefArrayFieldValue,
  },
  props: {
    field: {
      type: Object,
      required: true,
    },
    color: {
      type: String,
      default: () => "primary",
    },
  },
  computed: {
    cellTypeComponentName() {
      return {
        STRING: "StringDisplay",
        BOOL: "StringDisplay",
        INT: "StringDisplay",
        DECIMAL: "StringDisplay",
        TEXT: "TextDisplay",
        JSONB: "TextDisplay",
        FILE: "FileDisplay",
        DATE: "StringDisplay",
        UUID: "StringDisplay",
        BOOL_ARRAY: "StringDisplay",
        UUID_ARRAY: "StringDisplay",
        STRING_ARRAY: "StringDisplay",
        TEXT_ARRAY: "StringDisplay",
        INT_ARRAY: "StringDisplay",
        DECIMAL_ARRAY: "StringDisplay",
        DATE_ARRAY: "StringDisplay",
        DATETIME: "StringDisplay",
        DATETIME_ARRAY: "StringDisplay",
        JSONB_ARRAY: "StringDisplay",
        REF_ARRAY: "RefArrayFieldValue",
        ONTOLOGY_ARRAY: "StringDisplay",
        REF: "ObjectDisplay",
        REFBACK: "RefBackFieldValue",
        HEADING: "StringDisplay",
        ONTOLOGY: "ObjectDisplay",
      }[this.field.meta.columnType];
    },
    isEmptyValue() {
      return (
        this.field.value === undefined ||
        this.field.value === null ||
        (Array.isArray(this.field.value) && this.field.value.length === 0) ||
        (this.field.value &&
          Object.keys(this.field.value).length === 0 &&
          Object.getPrototypeOf(this.field.value) === Object.prototype)
      );
    },
  },
};
</script>
