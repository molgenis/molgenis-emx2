<template>
  <span v-if="row[col.id] && Array.isArray(row[col.id])">
    <span v-for="(val, idx) in row[col.id]" :key="idx">
      <HyperlinkDisplay
        v-if="col.columnType === 'HYPERLINK_ARRAY'"
        :data="val"
      />
      <EmailDisplay v-else-if="col.columnType === 'EMAIL_ARRAY'" :data="val" />
      <span v-else>
        {{ getLabel(col) ? applyJsTemplate(getLabel(col), val) : val }}
      </span>
    </span>
  </span>
  <span v-else-if="row[col.id]">
    <HyperlinkDisplay
      v-if="col.columnType === 'HYPERLINK'"
      :data="row[col.id]"
    />
    <EmailDisplay v-else-if="col.columnType === 'EMAIL'" :data="row[col.id]" />
    <span v-else>
      {{ getValue(col, row) }}
    </span>
  </span>
</template>

<script>
import HyperlinkDisplay from "./cellTypes/HyperlinkDisplay.vue";
import EmailDisplay from "./cellTypes/EmailDisplay.vue";
import { applyJsTemplate } from "../utils";

export default {
  props: {
    row: Object,
    col: Object,
  },
  components: {
    HyperlinkDisplay,
    EmailDisplay,
  },
  methods: {
    getValue(col, row) {
      return this.getLabel(col)
        ? applyJsTemplate(row[col.id], getLabel(col))
        : row[col.id];
    },
    getLabel(col) {
      return col.refLabel ? col.refLabel : col.refLabelDefault;
    },
  },
};
</script>
