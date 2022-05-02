<template>
  <div class="m-2 showcontainer row">
    <detailViewSectionFieldLabel
      class="col-4"
      :label="field.meta.name"
      :tooltip="field.description"
      :color="color"
    ></detailViewSectionFieldLabel>
    <div class="col-8">
      <component
        v-if="!isEmptyValue"
        :is="fieldTypeComponentName"
        :data="field.value"
        :metaData="field.meta"
        :color="color"
      />
      <div v-else>N/A</div>
    </div>
  </div>
</template>

<script>
import { StringDisplay, FileDisplay, ObjectDisplay } from "molgenis-components";

export default {
  name: "SectionField",
  components: {
    StringDisplay,
    FileDisplay,
    ObjectDisplay,
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
    fieldTypeComponentName() {
      return {
        STRING: "StringDisplay",
        BOOL: "StringDisplay",
        INT: "StringDisplay",
        DECIMAL: "StringDisplay",
        FILE: "FileDisplay",
        DATE: "StringDisplay",
        UUID: "StringDisplay",
        DATETIME: "StringDisplay",
        HEADING: "StringDisplay",
        JSONB: "detailViewTextFieldValue",
        TEXT: "DetailViewTextFieldValue",
        BOOL_ARRAY: "DetailViewSectionArrayValue",
        UUID_ARRAY: "DetailViewSectionArrayValue",
        STRING_ARRAY: "DetailViewSectionArrayValue",
        TEXT_ARRAY: "DetailViewSectionArrayValue",
        INT_ARRAY: "DetailViewSectionArrayValue",
        DECIMAL_ARRAY: "DetailViewSectionArrayValue",
        DATE_ARRAY: "DetailViewSectionArrayValue",
        DATETIME_ARRAY: "DetailViewSectionArrayValue",
        JSONB_ARRAY: "DetailViewSectionArrayValue",
        ONTOLOGY_ARRAY: "DetailViewSectionArrayValue",
        REF_ARRAY: "DetailViewRefArrayFieldValue",
        REF: "DetailViewRefFieldValue",
        REFBACK: "DetailViewRefBackFieldValue",
        ONTOLOGY: "DetailViewOntologyFieldValue",
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
