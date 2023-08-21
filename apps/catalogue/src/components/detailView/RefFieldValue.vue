<template>
  <RouterLink
    :to="{
      name: convertToPascalCase(metaData.refTable) + '-details',
      params: routerParams,
    }"
    ><ObjectDisplay :data="data" :metaData="metaData" />
  </RouterLink>
</template>

<script>
import {
  ObjectDisplay,
  convertToPascalCase,
  deepClone,
} from "molgenis-components";

export default {
  name: "RefFieldValue",
  components: { ObjectDisplay },
  props: {
    data: {
      type: [Object, Array],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
  },
  computed: {
    routerParams() {
      if (this.data) {
        const result = deepClone(this.data);
        Object.keys(result).forEach((key) => {
          if (result[key] && typeof result[key] === "object") {
            if (result[key].id) result[key] = result[key].id;
            if (result[key].name) result[key] = result[key].name;
          }
        });
        return result;
      }
    },
  },
  methods: {
    convertToPascalCase,
  },
};
</script>
