<template>
  <div v-if="hasTemplate">{{ asTemplate }}</div>
  <div v-else>{{ asDotSeparatedString }}</div>
</template>

<script>
export default {
  name: "ObjectDisplay",
  props: {
    data: {
      type: [Object],
      required: true,
    },
    metaData: {
      type: [Object],
      required: true,
    },
  },
  computed: {
    hasTemplate() {
      return !!this.metaData.refLabel;
    },
    asTemplate() {
      const names = Object.keys(this.data);
      const vals = Object.values(this.data);
      try {
        return new Function(
          ...names,
          "return `" + this.metaData.refLabel + "`;"
        )(...vals);
      } catch (err) {
        const namesString = JSON.stringify(names);
        const valsString = JSON.stringify(vals);
        return `${err.message} we got keys: ${namesString} vals: ${valsString} and template: ${this.metaData.refLabel}`;
      }
    },
    asDotSeparatedString() {
      let result = "";
      Object.keys(this.data).forEach((key) => {
        if (this.data[key] === null) {
          //nothing
        } else if (typeof this.data[key] === "object") {
          result += this.flattenObject(this.data[key]);
        } else {
          result += "." + this.data[key];
        }
      });
      return result.replace(/^\./, "");
    },
  },
};
</script>
