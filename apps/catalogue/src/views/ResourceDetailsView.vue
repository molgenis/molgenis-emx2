<template>
  <div class="container bg-white" id="_top" v-if="resourceData" :key="filter">
    <RowButton
      v-if="canEdit"
      type="edit"
      class="float-right pt-1"
      @edit="isEditModalShown = true"
    />
    <EditModal
      v-if="canEdit"
      id="resource-edit-modal"
      :tableId="tableId"
      :pkey="primaryTableKey"
      :isModalShown="isEditModalShown"
      @close="isEditModalShown = false"
    />
    <ButtonAlt @click="toggleNA" class="float-right text-white">
      {{ hideNA ? "Show" : "Hide" }} empty fields (N/A)
    </ButtonAlt>
    <ResourceHeader
      :resource="resourceData"
      :headerCss="'bg-' + color + ' text-white'"
      :tableLabel="tableMetadata.label"
    />
    <div class="row p-2">
      <div
        :class="
          'border border-' +
          color +
          ' ' +
          (sectionLabels.length > 1 ? 'col-10' : 'col-12')
        "
        class="p-0"
      >
        <div v-for="section in sections" :key="section.meta.label">
          <section-card
            :section="section"
            :hideNA="hideNA"
            :color="color"
            :showCardHeader="sections.length > 1"
          />
        </div>
      </div>
      <section-index
        v-if="sectionLabels.length > 1"
        class="col-2"
        :names="sectionLabels"
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
import { ButtonAlt, RowButton, Client, EditModal } from "molgenis-components";
import ResourceHeader from "../components/ResourceHeader.vue";
import SectionIndex from "../components/detailView/SectionIndex.vue";
import SectionCard from "../components/detailView/SectionCard.vue";
import { mapActions, mapGetters } from "vuex";

export default {
  name: "ResourceDetailsView",
  components: {
    ButtonAlt,
    EditModal,
    ResourceHeader,
    RowButton,
    SectionIndex,
    SectionCard,
  },
  props: {
    color: { type: String, default: "primary" },
    tableId: { type: String, required: true }, // resource table name
    filter: { type: Object, required: true }, // resource id filter
  },
  data() {
    return {
      client: null,
      hideNA: true,
      tableMetadata: null,
      resourceData: null,
      isEditModalShown: false,
    };
  },
  computed: {
    ...mapGetters(["canEdit"]),
    sections() {
      const comparePosition = (a, b) => a.position < b.position;
      const isHeading = (meta) => meta.columnType === "HEADING";
      const isNonSystemField = (meta) => !meta.id.startsWith("mg_");

      return this.tableMetadata.columns
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
    sectionLabels() {
      return this.sections.map((card) => card.meta.label);
    },
    primaryTableKey() {
      return this.tableMetadata.columns.reduce((accum, col) => {
        if (col.key == 1 && this.resourceData[col.id]) {
          accum[col.id] = this.resourceData[col.id];
        }
        return accum;
      }, {});
    },
  },
  methods: {
    ...mapActions(["reloadMetadata"]),
    toggleNA() {
      this.hideNA = !this.hideNA;
    },
    async reload() {
      this.client = Client.newClient();
      this.reloadMetadata();
      this.tableMetadata = await this.client.fetchTableMetaData(this.tableId);
      this.resourceData = (
        await this.client.fetchTableDataValues(this.tableId, {
          filter: this.filter,
        })
      )[0];
    },
  },
  watch: {
    async filter() {
      await this.reload();
    },
  },
  async mounted() {
    await this.reload();
  },
};
</script>
