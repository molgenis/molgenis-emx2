<template>
  <span v-if="row[col.name] && Array.isArray(row[col.name])">
    <span v-for="val in row[col.name]" :key="val">
      {{ col.refJsTemplate ? applyJsTemplate(col.refJsTemplate, val) : val }}
    </span>
  </span>
  <span v-else-if="row[col.name]">
    {{
      col.refJsTemplate
        ? applyJsTemplate(col.refJsTemplate, row[col.name])
        : row[col.name]
    }}
  </span>
</template>

<script>
export default {
  props: {
    row: Array,
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
