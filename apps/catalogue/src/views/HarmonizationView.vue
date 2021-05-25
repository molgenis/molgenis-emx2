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
          <th class="table-label text-nowrap" scope="row">
            {{ variable.name }}
          </th>
          <td
            v-for="cohort in cohorts"
            :key="cohort.acronym"
            class="colored-grid-cell"
            :class="'table-' + getMatchStatus(variable, cohort.acronym)"
          >
            <!-- {{getMatchStatus(variable, cohort.acronym)}} -->
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
    getMatchStatus(variable, cohortAcronym) {
      if (!variable.repeats) {
        if (
          !this.harmonizationGrid[variable.name] ||
          !this.harmonizationGrid[variable.name][cohortAcronym]
        ) {
          return "danger"; // not mapped
        }
        const match = this.harmonizationGrid[variable.name][cohortAcronym];
        switch (match) {
          case "zna":
            return "danger";
          case "partial":
            return "success";
          case "complete":
            return "success";
          default:
            return "danger";
        }
      } else {
        const allVars = variable.repeats.concat([variable])
        const mappedRepeats = allVars.map((repeat) => {
          if (
            !this.harmonizationGrid[repeat.name] ||
            !this.harmonizationGrid[repeat.name][cohortAcronym]
          ) {
            return "danger"; // not mapped
          }
          const match = this.harmonizationGrid[repeat.name][cohortAcronym];
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
        });

        return mappedRepeats.filter((mr) => mr === "success").lenght
          ? "success" // if all repeats are mapped
          : mappedRepeats.includes("success") || // if some repeats are (partial) mapped
            mappedRepeats.includes("warning")
          ? "success"
          : "danger"; // if none of the repeats are mapped
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

.table-bordered th,
.table-bordered td {
  border: 1px solid #6c757d;
}
</style>
