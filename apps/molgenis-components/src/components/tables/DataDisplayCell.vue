<template>
  <component
    v-if="!isEmpty"
    :is="cellTypeComponentName"
    :data="data"
    :metadata="metadata"
  />
</template>

<script lang="ts">
import { defineComponent } from "vue";
import StringDisplay from "./cellTypes/StringDisplay.vue";
import FileDisplay from "./cellTypes/FileDisplay.vue";
import TextDisplay from "./cellTypes/TextDisplay.vue";
import ListDisplay from "./cellTypes/ListDisplay.vue";
import ObjectDisplay from "./cellTypes/ObjectDisplay.vue";
import EmailDisplay from "./cellTypes/EmailDisplay.vue";
import HyperlinkDisplay from "./cellTypes/HyperlinkDisplay.vue";
import {
  isMultiValuedType,
  isOntologyType,
  isSingleRefType,
} from "../../../../metadata-utils/src/fieldHelpers";

const typeMap: { [key: string]: string } = {
  FILE: "FileDisplay",
  TEXT: "TextDisplay",
  JSON: "TextDisplay",
  EMAIL: "EmailDisplay",
  HYPERLINK: "HyperlinkDisplay",
};

function resolveDisplayComponentName(columnType: string): string {
  if (isMultiValuedType(columnType)) {
    return "ListDisplay";
  } else if (isSingleRefType(columnType) || isOntologyType(columnType)) {
    return "ObjectDisplay";
  } else {
    return typeMap[columnType] || "StringDisplay";
  }
}

export default defineComponent({
  name: "DataDisplayCell",
  components: {
    StringDisplay,
    FileDisplay,
    TextDisplay,
    ListDisplay,
    ObjectDisplay,
    EmailDisplay,
    HyperlinkDisplay,
  },
  props: {
    data: {
      type: [String, Object, Array, Number, Boolean],
      required: false,
    },
    metadata: {
      type: Object,
      required: true,
    },
  },
  computed: {
    cellTypeComponentName() {
      return resolveDisplayComponentName(this.metadata.columnType);
    },
    isEmpty() {
      return (
        this.data === undefined ||
        this.data === null ||
        (Array.isArray(this.data) && this.data.length === 0)
      );
    },
  },
});
</script>
