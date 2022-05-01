<template>
  <div>
    <key-value-block
      v-if="subcohort"
      :heading="'Subpopulations: ' + subcohort.name"
      :items="details"
    ></key-value-block>

    <grid-block heading="Quantitative information" v-if="subcohort.counts">
      <table-display
        :isClickable="false"
        :columns="[
          { name: 'year', label: 'Year' },
          { name: 'ageband', label: 'Age band' },
          { name: 'gender', label: 'Gender' },
          { name: 'N', label: 'N' },
        ]"
        :rows="subcohort.counts"
      ></table-display>
    </grid-block>
  </div>
</template>

<script>
import {
  KeyValueBlock,
  GridBlock,
  TableDisplay,
} from "@molgenis/molgenis-components";
import { startEndYear } from "../../../../../../store/filters";
import query from "../../../../../../store/gql/subcohortDetails.gql";

export default {
  name: "SubCohort",
  components: { KeyValueBlock, GridBlock, TableDisplay },
  async asyncData({ params, $axios, store, redirect }) {
    if (!params.subCohort) {
      redirect({ to: "cohorts/" + params.cohort });
      return;
    }

    const resp = await $axios({
      url: store.state.schema + "/graphql",
      method: "post",
      data: {
        query,
        variables: { pid: params.cohort, name: params.subCohort },
      },
    }).catch((e) => console.log(e));

    if (!resp) return;

    return { subcohort: resp.data.data.Subcohorts[0] };
  },
  computed: {
    details() {
      return [
        {
          label: "",
          value: this.subcohort.description,
        },
        {
          label: "Number of participants",
          value: this.subcohort.numberOfParticipants,
        },
        {
          label: "Age categories",
          value: this.subcohort.ageGroups
            ? this.subcohort.ageGroups.map((ag) => ag.name)
            : [],
        },
        {
          label: "Start/end year: ",
          value: startEndYear(
            this.subcohort.inclusionStart,
            this.subcohort.inclusionEnd
          ),
        },
        {
          label: "Population",
          value: this.subcohort.countries
            ? this.subcohort.countries.map((c) => c.name)
            : [],
        },
        {
          label: "Main medical condition",
          value: this.subcohort.mainMedicalCondition
            ? this.subcohort.mainMedicalCondition
                .map((mmc) => mmc.name)
                .join(",")
            : "",
        },
        {
          label: "Other inclusion criteria",
          value: this.subcohort.inclusionCriteria,
        },
        {
          label: "Comorbidity",
          value: this.subcohort.comorbidity
            ? this.subcohort.comorbidity.map((c) => c.name)
            : [],
        },
      ];
    },
  },
};
</script>
