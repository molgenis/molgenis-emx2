<template>
  <div>
    <div v-for="(listItem, index) in data" :key="index">
      <object-display
        v-if="useObjectDisplay"
        :data="listItem"
        :metaData="metaData"
      ></object-display>
      <string-display
        v-else
        :data="listItem"
        :metaData="metaData"
      ></string-display>
    </div>
  </div>
</template>

<script>
import ObjectDisplay from "./ObjectDisplay.vue";
import StringDisplay from "./StringDisplay.vue";

export default {
  name: "ListDisplay",
  components: { ObjectDisplay, StringDisplay },
  props: {
    data: {
      type: [Array],
      required: true,
    },
    metaData: {
      type: Object,
      required: true,
    },
  },
  computed: {
    useObjectDisplay() {
      return ["REF_ARRAY", "ONTOLOGY_ARRAY", "REFBACK"].includes(
        this.metaData.columnType
      );
    },
  },
};
</script>
