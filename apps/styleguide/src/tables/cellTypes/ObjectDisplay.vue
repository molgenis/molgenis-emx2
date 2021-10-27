<template>
  <div v-if="hasTemplate">asTemplate</div>
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
      const names = Object.keys(this.object);
      const vals = Object.values(this.object);
      try {
        return new Function(...names, "return `" + this.template + "`;")(
          ...vals
        );
      } catch (err) {
        const namesString = JSON.stringify(names);
        const valsString = JSON.stringify(vals);
        return `${err.message} we got keys: ${namesString} vals: ${valsString} and template: ${this.template}`;
      }
    },
    asDotSeparatedString() {
      let result = "";
      Object.keys(this.object).forEach((key) => {
        if (this.object[key] === null) {
          //nothing
        } else if (typeof this.object[key] === "object") {
          result += this.flattenObject(this.object[key]);
        } else {
          result += "." + this.object[key];
        }
      });
      return result.replace(/^\./, "");
    },
  },
};
</script>
