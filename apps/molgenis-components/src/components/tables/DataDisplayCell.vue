<template>
  <component v-if="!isEmpty" :is="cellTypeComponentName" :data="data" :metaData="metaData" />
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

const typeMap: { [key: string]: string } = {
  FILE: "FileDisplay",
  TEXT: "TextDisplay",
  REFBACK: "ListDisplay",
  REF: "ObjectDisplay",
  ONTOLOGY: "ObjectDisplay",
  EMAIL: "EmailDisplay",
  HYPERLINK: "HyperlinkDisplay",
};

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
    metaData: {
      type: Object,
      required: true,
    },
  },
  computed: {
    cellTypeComponentName() {
      return this.isArrayType ? "ListDisplay" : typeMap[this.metaData.columnType] || "StringDisplay";
    },
    isArrayType() {
      return this.metaData.columnType.includes("ARRAY") > 0;
    },
    isEmpty() {
      return this.data === undefined || this.data === null || (Array.isArray(this.data) && this.data.length === 0);
    },
  },
});
</script>
