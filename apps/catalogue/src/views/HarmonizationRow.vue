<template>
  <tr>
    <th class="table-label text-nowrap" scope="row">
      {{ variable.name }}
    </th>
    <td
      v-for="cohort in resources"
      :key="cohort.acronym"
      class="colored-grid-cell"
      :class="'table-' + getCellClass(cohort)"
    ></td>
  </tr>
</template>

<script>
import { mapActions } from "vuex";
export default {
  name: "HarmonizationRow",
  props: {
    variable: Object,
    resources: Array,
  },
  data() {
    return {
      resourceMappings: undefined,
    };
  },
  methods: {
    ...mapActions(["fetchMappings"]),
    getCellClass(cohort) {
      return this.resourceMappings ? this.getMatchStatus(cohort) : null;
    },
    async fetchData() {
      this.resourceMappings = await this.fetchMappings(this.variable);
    },
    getMatchStatus(resource) {
      if (this.variable.repeats) {
        const statusList = this.variable.repeats.map((repeatedVariable) => {
          const resourceMapping = this.resourceMappings.find((mapping) => {
            return (
              mapping.toVariable.name === repeatedVariable.name &&
              mapping.fromTable.release.resource.acronym === resource.acronym
            );
          });

          return resourceMapping ? resourceMapping.match.name : "zna";
        });

        if (statusList.includes("complete")) {
          return "success";
        } else if (statusList.includes("partial")) {
          return "success";
        } else {
          return "danger";
        }
      } else {
        const resourceMapping = this.resourceMappings.find((mapping) => {
          return (
            mapping.fromTable.release.resource.acronym === resource.acronym
          );
        });

        if (!resourceMapping) {
          return "danger";
        }

        switch (resourceMapping.match.name) {
          case "zna":
            return "danger";
          case "partial":
            return "success";
          case "complete":
            return "success";
          default:
            return "danger";
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
