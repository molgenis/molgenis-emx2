<template>
  <div>
    <key-value-block
      v-if="collectionEvent"
      :heading="'Collection event: ' + collectionEvent.name"
      :items="details"
    ></key-value-block>
  </div>
</template>

<style scoped></style>

<script>
import { PageHeader, GridBlock, KeyValueBlock } from "molgenis-components";
import { startEndYear } from "../../../../../../store/filters";
import query from "../../../../../../store/gql/collectionEvent.gql";

export default {
  name: "CollectionEvent",
  components: { PageHeader, GridBlock, KeyValueBlock },
  scrollToTop: true,
  async asyncData({ params, $axios, store }) {
    if (!params.collectionEvent) {
      redirect({ to: "cohorts/" + params.cohort });
      return;
    }
    const resp = await $axios({
      url: store.state.schema + "/graphql",
      method: "post",
      data: {
        query,
        variables: { pid: params.cohort, name: params.collectionEvent },
      },
    }).catch((e) => console.log(e));

    if (!resp) return;

    return { collectionEvent: resp.data.data.CollectionEvents[0] };
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
};
</script>
