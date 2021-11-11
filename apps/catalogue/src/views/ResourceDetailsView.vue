<template>
  <div class="container bg-white" id="_top" v-if="resourceData">
    <ButtonAlt @click="toggleNA" class="float-right text-white">
      {{ hideNA ? "Show" : "Hide" }} empty fields (N/A)
    </ButtonAlt>
    <ResourceHeader
      :resource="resourceData"
      :headerCss="'bg-' + color + ' text-white'"
      :table-name="table"
    />
    <div class="row p-2">
      <div :class="'border border-' + color" class="col-10 p-0">
        <div v-for="section in sections" :key="section.meta.name">
          <section-card :section="section" :hideNA="hideNA" :color="color" />
        </div>
      </div>
      <section-index
        v-if="sectionsNames.length > 0"
        class="col-2"
        :names="sectionsNames"
        :color="color"
      />
    </div>
  </div>
</template>

<style>
.labelcontainer .tooltip {
  visibility: hidden;
}

.labelcontainer:hover .tooltip {
  visibility: visible;
}
</style>

<script>
import { TableMixin, ButtonAlt } from "@mswertz/emx2-styleguide";
import ResourceHeader from "../components/ResourceHeader";
import SectionIndex from "../components/detailView/SectionIndex";
import SectionCard from "../components/detailView/SectionCard";

export default {
  extends: TableMixin,
  components: {
    ResourceHeader,
    ButtonAlt,
    SectionIndex,
    SectionCard,
  },
  props: {
    color: { type: String, default: "primary" },
  },
  data() {
    return {
      hideNA: true,
    };
  },
  computed: {
    resourceData() {
      return this.data[0];
    },
    sections() {
      const comparePosition = (a, b) => a.position < b.position;
      const isHeading = (meta) => meta.columnType === "HEADING";
      const isNonSystemField = (meta) => !meta.name.startsWith("mg_");

      return this.tableMetadata.columns
        .filter(isNonSystemField)
        .sort(comparePosition)
        .reduce((accum, item) => {
          if (isHeading(item) || !accum.length) {
            accum.push({ meta: item, fields: [] });
          } else {
            accum.at(-1).fields.push({
              meta: { ...item, primaryTableKey: this.primaryTableKey },
              value: this.resourceData[item.name],
            });
          }
          return accum;
        }, []);
    },
    sectionsNames() {
      return this.sections.map((card) => card.meta.name);
    },
    primaryTableKey() {
      return this.tableMetadata.columns.reduce((accum, col) => {
        if (col.key == 1 && this.resourceData[col.name]) {
          accum[col.name] = this.resourceData[col.name];
        }
        return accum;
      }, {});
    },
  },
  methods: {
    toggleNA() {
      this.hideNA = !this.hideNA;
    },
  },
};
</script>
