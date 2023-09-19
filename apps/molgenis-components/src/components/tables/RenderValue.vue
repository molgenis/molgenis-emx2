<template>
  <span v-if="row[col.name] && Array.isArray(row[col.name])">
    <span v-for="(val, idx) in row[col.name]" :key="idx" class="pr-1">
      <HyperlinkDisplay
        v-if="col.columnType === 'HYPERLINK_ARRAY'"
        :data="val"
      />
      <EmailDisplay v-else-if="col.columnType === 'EMAIL_ARRAY'" :data="val" />
      <span v-else>
        {{ getRefLabel(col) ? applyJsTemplate(val, getRefLabel(col)) : val }}
      </span>
    </span>
  </span>
  <span v-else-if="row[col.name]">
    <HyperlinkDisplay
      v-if="col.columnType === 'HYPERLINK'"
      :data="row[col.name]"
    />
    <EmailDisplay
      v-else-if="col.columnType === 'EMAIL'"
      :data="row[col.name]"
    />
    <FileDisplay
      v-else-if="col.columnType === 'FILE'"
      :data="row[col.name]"
      :metaData="col"
    />
    <span v-else>
      {{ getValue(col, row) }}
    </span>
  </span>
</template>

<script>
import HyperlinkDisplay from "./cellTypes/HyperlinkDisplay.vue";
import EmailDisplay from "./cellTypes/EmailDisplay.vue";
import FileDisplay from "./cellTypes/FileDisplay.vue";
import { applyJsTemplate } from "../utils";

export default {
  props: {
    row: Object,
    col: Object,
  },
  components: {
    HyperlinkDisplay,
    EmailDisplay,
    FileDisplay,
  },
  methods: {
    applyJsTemplate,
    getValue(col, row) {
      return this.getRefLabel(col)
        ? applyJsTemplate(row[col.name], this.getRefLabel(col))
        : row[col.name];
    },
    getRefLabel(col) {
      return col.refLabel ? col.refLabel : col.refLabelDefault;
    },
  },
};
</script>
