<template>
  <span v-if="row[col.id] && Array.isArray(row[col.id])">
    <span v-for="(val, idx) in row[col.id]" :key="idx" class="pryarn-1">
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
  <span v-else-if="row[col.id]">
    <HyperlinkDisplay
      v-if="col.columnType === 'HYPERLINK'"
      :data="row[col.id]"
    />
    <EmailDisplay v-else-if="col.columnType === 'EMAIL'" :data="row[col.id]" />
    <FileDisplay
      v-else-if="col.columnType === 'FILE'"
      :data="row[col.id]"
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
        ? applyJsTemplate(row[col.id], this.getRefLabel(col))
        : row[col.id];
    },
    getRefLabel(col) {
      return col.refLabel ? col.refLabel : col.refLabelDefault;
    },
  },
};
</script>
