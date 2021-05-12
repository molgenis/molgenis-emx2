<template>
  <div class="mt-3">
    <table class="table table-bordered table-sm">
      <thead>
        <tr>
          <th scope="col"></th>
          <th
            class="rotated-text text-nowrap"
            scope="col"
            v-for="cohort in cohorts"
            :key="cohort.acronym"
          >
            <div>
              <span class="table-label">{{ cohort.acronym }}</span>
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="variable in variables" :key="variable.name">
          <th class="table-label text-nowrap" scope="row">{{ variable.name }}</th>
          <td
            v-for="cohort in cohorts"
            :key="cohort.acronym"
            class="colored-grid-cell"
            :class="'table-' + getMatchStatus(variable.name, cohort.acronym)"
          >
            <!-- {{getMatchStatus(variable.name, cohort.acronym)}} -->
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
export default {
  name: "HarmonizationView",
  computed: {
    ...mapGetters(["cohorts", "variables", "harmonizationGrid"]),
  },
  methods: {
    ...mapActions(["fetchCohorts", "fetchMappings"]),
    getMatchStatus(variableName, cohortAcronym) {
      if (
        !this.harmonizationGrid[variableName] ||
        !this.harmonizationGrid[variableName][cohortAcronym]
      ) {
        return "danger"; // not mapped
      }
      const match = this.harmonizationGrid[variableName][cohortAcronym];
      switch (match) {
        case "zna":
          return "danger";
        case "partial":
          return "warning";
        case "complete":
          return "success";
        default:
          return "danger";
      }
    },
  },
  watch: {
    variables() {
      this.fetchMappings();
    },
  },
  mounted() {
    this.fetchCohorts();
    this.fetchMappings();
  },
};
</script>

<style scoped>
th.rotated-text {
  height: 13rem;
  padding: 0;
}
th.rotated-text > div {
  transform: translate(7px, 4px) rotate(270deg);
  width: 1.4rem;
}
th.rotated-text > div > span {
  padding: 5px 10px;
}

td.colored-grid-cell {
  padding: 0.97rem;
}

.table-label {
  font-size: 0.8rem;
}

.table-bordered th, .table-bordered td {
  border: 1px solid #6c757d;
}
</style>
