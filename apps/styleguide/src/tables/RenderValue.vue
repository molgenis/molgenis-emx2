<template>
  <span v-if="row[col.name] && Array.isArray(row[col.name])">
    <span v-for="(val, idx) in row[col.name]" :key="idx">
      {{ col.refLabel ? applyJsTemplate(col.refLabel, val) : val }}
    </span>
  </span>
  <span v-else-if="row[col.name]">
    {{
      col.refLabel
        ? applyJsTemplate(col.refLabel, row[col.name])
        : row[col.name]
    }}
  </span>
</template>

<script>
export default {
  props: {
    row: Object,
    col: Object,
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
    },
  },
};
</script>
