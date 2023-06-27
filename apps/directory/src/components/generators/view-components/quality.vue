<template>
  <tr v-if="attribute.value && attribute.value.length">
    <th scope="row" class="pr-1 align-top text-nowrap">
      {{ attribute.label }}
    </th>
    <td>
      <quality-column
        :qualities="attribute.value"
        :spacing="0"
        :quality-info="qualityStandardsDictionary"
      ></quality-column>
    </td>
  </tr>
</template>

<script>
import { useQualitiesStore } from "../../../stores/qualitiesStore";
import QualityColumn from "../../tables/QualityColumn.vue";

export default {
  setup() {
    const qualitiesStore = useQualitiesStore();
    return { qualitiesStore };
  },
  components: {
    QualityColumn,
  },
  props: {
    /**
     * Collection or Biobank with a quality property
     */
    attribute: {
      type: Object,
      required: true,
      default: () => {},
    },
  },
  computed: {
    qualityStandardsDictionary() {
      return this.qualitiesStore.qualityStandardsDictionary;
    },
  },
  mounted() {
    this.qualitiesStore.getQualityStandardInformation();
  },
};
</script>
