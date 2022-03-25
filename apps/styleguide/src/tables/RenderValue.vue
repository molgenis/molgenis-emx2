<template>
  <span v-if="row[col.id] && Array.isArray(row[col.id])">
    <span v-for="(val, idx) in row[col.id]" :key="idx">
      <a v-if="col.columnType == 'HYPERLINK_ARRAY'" :href="val" target="_blank">
        {{ val }}
      </a>
      <a v-else-if="col.columnType == 'EMAIL_ARRAY'" :href="`mailto:${val}`">
        {{ val }}
      </a>
      <span v-else>
        {{ col.refLabel ? applyJsTemplate(col.refLabel, val) : val }}
      </span>
    </span>
  </span>
  <span v-else-if="row[col.id]">
    <a v-if="col.columnType == 'HYPERLINK'" :href="row[col.id]" target="_blank">
      {{ row[col.id] }}
    </a>
    <a v-else-if="col.columnType == 'EMAIL'" :href="`mailto:${row[col.id]}`">
      {{ row[col.id] }}
    </a>
    <span v-else>
      {{
        col.refLabel ? applyJsTemplate(col.refLabel, row[col.id]) : row[col.id]
      }}
    </span>
  </span>
</template>

<script>
export default {
  props: {
    row: Object,
    col: Object
  },
  methods: {
    applyJsTemplate(template, object) {
      const names = Object.keys(object);
      const vals = Object.values(object);
      try {
        return new Function(...names, "return `" + template + "`;")(...vals);
      } catch (err) {
        return (
          err.message +
          " we got keys:" +
          JSON.stringify(names) +
          " vals:" +
          JSON.stringify(vals) +
          " and template: " +
          template
        );
      }
    }
  }
};
</script>
