<template>
  <RouterLink
    :to="{
      name: metaData.refTable + '-details',
      params: data,
    }"
  >
    <ObjectDisplay :data="data" :metaData="metaData" />
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
    metaData: {
      type: Object,
      required: true,
    },
  },
};
</script>
