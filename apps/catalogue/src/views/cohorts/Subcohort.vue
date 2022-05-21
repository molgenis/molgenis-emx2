<template>
  <div class="container">
    <grid-block v-if="cohortData">
      <page-header
        :title="cohortData.name"
        :logoUrl="cohortData.logo.url"
      ></page-header>
    </grid-block>

    <key-value-block
      v-if="subcohort"
      :heading="'Subpopulations: ' + subcohort.name"
      :items="details"
    ></key-value-block>
    <grid-block heading="Quantitative information" v-if="subcohort.counts">
      <table-display
        :isClickable="false"
        :columns="[
          { name: 'ageGroup', label: 'Age group' },
          { name: 'N_total', label: 'N total' },
          { name: 'N_female', label: 'N female' },
          { name: 'N_male', label: 'N male' },
        ]"
        :rows="quantitativeInformation"
      ></table-display>
    </grid-block>
  </div>
</template>

<style scoped></style>

<script>
import { fetchById } from "../../store/repository/repository";
import {
  PageHeader,
  GridBlock,
  KeyValueBlock,
  TableDisplay,
} from "@mswertz/emx2-styleguide";
import { startEndYear } from "../../filters";

export default {
  name: "SubCohort",
  components: { PageHeader, GridBlock, KeyValueBlock, TableDisplay },
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
      subcohort: null,
      cohortData: null,
    };
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
    quantitativeInformation() {
      return this.subcohort.counts.map(count => {
        count.ageGroup = count.ageGroup.name
        return count
      })
    },
  },
  mounted: async function () {
    fetchById("cohortDetails.gql", "Cohorts", {
      pid: this.$route.params.cohort,
    }).then((data) => (this.cohortData = data));

    this.subcohort = await fetchById("subcohortDetails.gql", "Subcohorts", {
      pid: this.$route.params.cohort,
      name: this.name,
    });
  },
};
</script>
