<template>
  <tr>
    <th class="table-label text-nowrap" scope="row">
      {{ variable.name }}
    </th>
    <td
      v-for="cohort in cohortsWithStatus"
      :key="cohort.acronym"
      class="colored-grid-cell"
      :class="'table-' + getCellClass(cohort)"
    >
      {{ cellValue(cohort.cellStatus) }}
    </td>
  </tr>
</template>

<script>
import { mapActions } from "vuex";
export default {
  name: "HarmonizationRow",
  props: {
    variable: Object,
    cohorts: Array,
  },
  data() {
    return {
      cohortMappings: undefined,
      cohortsWithStatus: JSON.parse(JSON.stringify(this.cohorts)), // deep copy for inernal use
    };
  },
  methods: {
    ...mapActions(["fetchMappings"]),
    getCellClass(cohort) {
      return this.cohortMappings ? this.getMatchStatus(cohort) : null;
    },
    async fetchData() {
      this.cohortMappings = await this.fetchMappings(this.variable);
    },
    cellValue(status) {
      switch (status) {
        case "danger":
          return "x";
        case "success":
          return "o";
        default:
          return "o";
      }
    },
    getMatchStatus(cohort) {
      if (this.variable.repeats) {
        const statusList = this.variable.repeats.map((repeatedVariable) => {
          const cohortMapping = this.cohortMappings.find((mapping) => {
            return (
              mapping.toVariable.name === repeatedVariable.name &&
              mapping.fromTable.release.resource.acronym === cohort.acronym
            );
          });

          return cohortMapping ? cohortMapping.match.name : "zna";
        });

        if (statusList.includes("complete")) {
          cohort.cellStatus = "success";
          return cohort.cellStatus;
        } else if (statusList.includes("partial")) {
          cohort.cellStatus = "success";
          return cohort.cellStatus;
        } else {
          cohort.cellStatus = "danger";
          return cohort.cellStatus;
        }
      } else {
        const cohortMapping = this.cohortMappings.find((mapping) => {
          return mapping.fromTable.release.resource.acronym === cohort.acronym;
        });

        if (!cohortMapping) {
          cohort.cellStatus = "danger";
          return cohort.cellStatus;
        }

        switch (cohortMapping.match.name) {
          case "zna":
            cohort.cellStatus = "danger";
            return cohort.cellStatus;
          case "partial":
            cohort.cellStatus = "success";
            return cohort.cellStatus;
          case "complete":
            cohort.cellStatus = "success";
            return cohort.cellStatus;
          default:
            cohort.cellStatus = "danger";
            return cohort.cellStatus;
        }
      }
    },
  },
  async mounted() {
    await this.fetchData();
  },
};
</script>

<style scoped>
td.colored-grid-cell {
  text-align: center;
  width: 1.8rem;
  height: 1.8rem;
}

.table-label {
  font-size: 0.8rem;
}

.table-bordered th,
.table-bordered td {
  border: 1px solid #6c757d;
}
</style>
