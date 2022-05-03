<template>
  <ObjectDisplay
    :class="'font-weight-bold mr-2 mb-2 badge bade-lg badge-' + color"
    :data="data"
    :metaData="metaData"
  />
</template>

<script>
import { ObjectDisplay } from "molgenis-components";

export default {
  name: "OntologyFieldValue",
  components: { ObjectDisplay },
  props: {
    data: {
      type: [Object],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
    color: {
      type: String,
      default: () => "primary",
    },
  },
};
</script>
