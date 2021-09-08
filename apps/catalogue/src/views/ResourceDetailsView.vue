<template>
  <div class="container bg-white" id="_top">
    <ButtonAlt @click="toggleNA" class="float-right text-white">
      {{ hideNA ? "Show" : "Hide" }} empty fields (N/A)
    </ButtonAlt>
    <ResourceHeader
        :resource="data[0]"
        :headerCss="'bg-' + color + ' text-white'"
        :table-name="table"
    />
    <ReportBlock
        :hideNA="hideNA"
        :color="color"
        :row="data[0]"
        :table-metadata="tableMetadata"
        v-if="tableMetadata"
        :showColumns="
        tableMetadata.columns
          .map((c) => c.name)
          .filter((name) => !name.startsWith('mg_'))
      "
    />
  </div>
</template>

<script>
import { TableMixin, ButtonAlt } from "@mswertz/emx2-styleguide";
import ReportBlock from "../components/ReportBlock";
import ResourceHeader from "../components/ResourceHeader";

export default {
  extends: TableMixin,
  props: {
    color: { type: String, default: "primary" },
  },
  data() {
    return {
      hideNA: true,
    };
  },
  methods: {
    toggleNA() {
      this.hideNA = !this.hideNA;
    },
  },
  components: { ReportBlock, ResourceHeader, ButtonAlt },
};
</script>
