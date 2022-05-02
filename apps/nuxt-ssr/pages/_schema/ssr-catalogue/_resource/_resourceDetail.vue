<template>
  <div>
    <div class="container bg-white" id="_top" v-if="resourceData">
      <ButtonAlt @click="toggleNA" class="float-right text-white">
        {{ hideNA ? "Show" : "Hide" }} empty fields (N/A)
      </ButtonAlt>
      <ResourceHeader
        :resource="resourceData"
        :headerCss="'bg-' + color + ' text-white'"
        :table-name="$route.params.resource"
      />
      <div class="row p-2">
        <div :class="'border border-' + color" class="col-10 p-0">
          <div v-for="section in sections" :key="section.meta.name">
            <detail-view-section-card
              :section="section"
              :hideNA="hideNA"
              :color="color"
              :showCardHeader="sections.length > 1"
            />
          </div>
        </div>
        <detail-view-section-index
          v-if="sectionsNames.length > 1"
          class="col-2"
          :names="sectionsNames"
          :color="color"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { Client, ButtonAlt } from "molgenis-components";

export default {
  components: { ButtonAlt },
  props: {
    color: { type: String, default: "primary" },
  },
  data() {
    return {
      hideNA: true,
    };
  },
  async asyncData({ params, $axios }) {
    const tableName = params.resource;
    const client = Client.newClient("/" + params.schema + "/graphql", $axios);
    const metaData = await client.fetchMetaData();
    const filter = { pid: { equals: params.resourceDetail } };
    const dataResponse = await client.fetchTableData(tableName, { filter });
    const resourceData = dataResponse[tableName][0];

    return { resourceData, metaData };
  },
  computed: {
    tableMetaData() {
      return this.metaData.tables.find(
        (t) => t.name === this.$route.params.resource
      );
    },
    sections() {
      const comparePosition = (a, b) => a.position < b.position;
      const isHeading = (meta) => meta.columnType === "HEADING";
      const isNonSystemField = (meta) => !meta.id.startsWith("mg_");

      if (!this.tableMetaData) return;

      return this.tableMetaData.columns
        .filter(isNonSystemField)
        .sort(comparePosition)
        .reduce((accum, item) => {
          // nest fields (data columns) withing sections (headings)
          if (isHeading(item)) {
            accum.push({ meta: item, fields: [] });
          } else {
            if (!accum.length) {
              accum.push({ meta: item, fields: [] });
            }
            accum.at(-1).fields.push({
              meta: { ...item, primaryTableKey: this.primaryTableKey },
              value: this.resourceData[item.id],
            });
          }
          return accum;
        }, []);
    },
    sectionsNames() {
      return this.sections.map((card) => card.meta.name);
    },
    primaryTableKey() {
      return this.tableMetaData.columns.reduce((accum, col) => {
        if (col.key == 1 && this.resourceData[col.id]) {
          accum[col.id] = this.resourceData[col.id];
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

<style>
.labelcontainer .tooltip {
  visibility: hidden;
}

.labelcontainer:hover .tooltip {
  visibility: visible;
}
</style>
