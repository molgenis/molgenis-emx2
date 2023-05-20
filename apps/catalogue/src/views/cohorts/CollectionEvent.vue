<template>
  <div class="container">
    <grid-block v-if="cohortData">
      <page-header
        :title="cohortData.name"
        :logoUrl="cohortData.logo.url"
      ></page-header>
    </grid-block>

    <key-value-block
      v-if="collectionEvent"
      :heading="'Collection event: ' + collectionEvent.name"
      :items="details"
    ></key-value-block>
  </div>
</template>

<style scoped></style>

<script>
import { fetchById } from "../../store/repository/repository";
import { PageHeader, GridBlock, KeyValueBlock } from "molgenis-components";
import { startEndYear } from "../../filters";

export default {
  name: "CollectionEvent",
  components: { PageHeader, GridBlock, KeyValueBlock },
  props: {
    cohort: {
      type: String,
      required: true,
    },
    name: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      cohortData: null,
      collectionEvent: null,
    };
  },
  computed: {
    details() {
      return [
        {
          label: "",
          value: this.collectionEvent.description,
        },
        {
          label: "Subcohorts",
          value: this.collectionEvent.subcohorts
            ? this.collectionEvent.subcohorts.map((item) => item.name)
            : [],
        },
        {
          label: "Number of participants",
          value: this.collectionEvent.numberOfParticipants,
        },
        {
          label: "Age categories",
          value: this.collectionEvent.ageGroups
            ? this.collectionEvent.ageGroups.map((ag) => ag.name)
            : [],
        },
        {
          label: "Start/end year: ",
          value: startEndYear(
            this.collectionEvent.startYear &&
              this.collectionEvent.startYear.name
              ? this.collectionEvent.startYear.name
              : null,
            this.collectionEvent.endYear && this.collectionEvent.endYear.name
              ? this.collectionEvent.endYear.name
              : null
          ),
        },
        {
          label: "Data Categories",
          value: this.collectionEvent.dataCategories
            ? this.collectionEvent.dataCategories.map((c) => c.name)
            : [],
        },
        {
          label: "Areas of information",
          value: this.collectionEvent.areasOfInformation
            ? this.collectionEvent.areasOfInformation.map((item) => item.name)
            : [],
        },
        {
          label: "Sample categories",
          value: this.collectionEvent.sampleCategories
            ? this.collectionEvent.sampleCategories.map((item) => item.name)
            : [],
        },
        {
          label: "Core variables",
          value: this.collectionEvent.coreVariables
            ? this.collectionEvent.coreVariables.map((item) => item.name)
            : [],
        },
      ];
    },
  },
  mounted: async function () {
    fetchById("cohortDetails", "Cohorts", {
      id: this.$route.params.cohort,
    }).then((data) => (this.cohortData = data));

    this.collectionEvent = await fetchById(
      "collectionEvent",
      "CollectionEvents",
      {
        id: this.$route.params.cohort,
        name: this.name,
      }
    );
  },
};
</script>
