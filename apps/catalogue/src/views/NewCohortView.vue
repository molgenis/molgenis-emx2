<template>
  <div v-if="cohort" class="container">
    <page-header-vue
      class="card block"
      :title="cohort.name"
      :logoUrl="cohort.logo.url"
    ></page-header-vue>

    <links-block
      class="card block"
      :items="[
        {
          label: 'Website',
          href: cohort.website,
        },
        {
          label: 'Contact',
          href: cohort.contactEmail,
        },
      ]"
    ></links-block>

    <div class="card-columns">
      <key-value-block
        class="card block"
        :items="[
          {
            label: 'Description',
            value: cohort.description,
          },
        ]"
      ></key-value-block>
      <key-value-block
        class="card block"
        :items="[
          {
            label: 'Marker paper',
            value: cohort.description,
          },
        ]"
      ></key-value-block>

      <div class="">
        <key-value-block
          class="card block"
          heading="General design"
          :items="generalDesignItems"
        ></key-value-block>
      </div>
    </div>

    <contacts-block
      class="card block"
      heading="Contributors"
      :items="cohort.contributors"
    ></contacts-block>

    <div class="row">
      <div class="col">
        <div class="block row">
          <p class="">Hier een lijst met logo's</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.block {
  margin-bottom: 1rem;
  padding: 1rem;
}

@media (min-width: 576px) {
  .card-columns {
    column-count: 2;
  }
}
</style>

<script>
import PageHeaderVue from "../components/blocks/PageHeader.vue";
import LinksBlock from "../components/blocks/LinksBlock.vue";
import KeyValueBlock from "../components/blocks/KeyValueBlock.vue";
import ContactsBlock from "../components/blocks/ContactsBlock.vue";
import { fetchById } from "../store/repository/cohortRepository";
import { fetchSchemaMetaData } from "../store/repository/metaDataRepository";
export default {
  name: "CohortView",
  components: { PageHeaderVue, LinksBlock, KeyValueBlock, ContactsBlock },
  data() {
    return {
      cohort: null,
      metaData: null,
    };
  },
  computed: {
    generalDesignItems() {
      return [
        {
          label: "Type",
          value: this.cohort.collectionType[0].name,
        },
        {
          label: "Design",
          value: this.cohort.design.name,
        },
        {
          label: "Collection type",
          value: this.cohort.collectionType[0].name,
        },
        {
          label: "Start/End year",
          value:
            (this.cohort.startYear || "N/A") +
            " - " +
            (this.cohort.endYear || "N/A"),
        },
        {
          label: "Population",
          value: "geen idee",
        },
        {
          label: "Number of participants",
          value: this.cohort.numberOfParticipants,
        },
        {
          label: "Age group at inclusion",
          value: "geen idee",
        },
      ];
    },
    cohortMetaData() {
      return this.metaData._schema.tables.find(
        (table) => table.name == "Cohorts"
      );
    },
  },
  methods: {
    async fetchData() {
      this.cohort = await fetchById(this.$route.params.pid);
    },
    async fetchMetaData() {
      this.metaData = await fetchSchemaMetaData();
    },
  },
  mounted: async function () {
    this.fetchData();
    // this.fetchMetaData();
  },
};
</script>
