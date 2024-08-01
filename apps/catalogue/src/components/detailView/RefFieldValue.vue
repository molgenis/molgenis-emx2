<template>
  <RouterLink
    :to="{
      name: metadata.refTableId + '-details',
      params: data,
    }"
    ><ObjectDisplay :data="data" :metadata="metadata" />
  </RouterLink>
</template>

<script>
import { ObjectDisplay } from "molgenis-components";

export default {
  name: "RefFieldValue",
  components: { ObjectDisplay },
  props: {
    data: {
      type: [Object, Array],
      required: true,
    },
    metadata: {
      type: Object,
      required: true,
    },
  },
};
</script>
