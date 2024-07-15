<template>
  <span v-if="hasTemplate">
    {{ asTemplate }}
  </span>
  <span v-else>
    {{ asDotSeparatedString }}
  </span>
</template>

<script>
import { flattenObject } from "../../utils";

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
      return !!this.metaData.refLabel || !!this.metaData.refLabelDefault;
    },
    asTemplate() {
      const ids = Object.keys(this.data);
      const vals = Object.values(this.data);
      const refLabel = this.metaData.refLabel
        ? this.metaData.refLabel
        : this.metaData.refLabelDefault;
      try {
        return new Function(...ids, "return `" + refLabel + "`;")(...vals);
      } catch (err) {
        const idsAsString = JSON.stringify(ids);
        const valsString = JSON.stringify(vals);
        return `${err.message} we got keys: ${idsAsString} vals: ${valsString} and template: ${refLabel}`;
      }
    },
    asDotSeparatedString() {
      let result = "";
      Object.keys(this.data).forEach((key) => {
        if (this.data[key] === null) {
          //nothing
        } else if (typeof this.data[key] === "object") {
          result += flattenObject(this.data[key]);
        } else {
          result += "." + this.data[key];
        }
      });
      return result.replace(/^\./, "");
    },
  },
};
</script>
