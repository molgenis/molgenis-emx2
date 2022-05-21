<template>
  <tr>
    <th class="table-label text-nowrap" scope="row">
      {{ variable.name }}
    </th>

    <harmonization-cell
      v-for="resource in resources"
      :key="resource.pid"
      class="colored-grid-cell"
      :status="getCellClass(resource)"
    />
  </tr>
</template>

<script>
import Vue from "vue";
import { mapActions } from "vuex";
import HarmonizationCell from "../components/harmonization/HarmonizationCell";

export default {
  name: "HarmonizationRow",
  components: { HarmonizationCell },
  props: {
    variable: Object,
    resources: Array,
  },
  data() {
    return {
      resourceMappings: undefined,
      resourceStatusMap: undefined,
    };
  },
  methods: {
    ...mapActions(["fetchMappings"]),
    getCellClass(cohort) {
      return this.resourceMappings ? this.getMatchStatus(cohort) : null;
    },
    async fetchData() {
      this.resourceMappings = await this.fetchMappings(this.variable);
      this.resourceStatusMap = this.resources.reduce((statusMap, resource) => {
        Vue.set(statusMap, resource.pid, this.getMatchStatus(resource));
        return statusMap;
      }, {});
    },
    getMatchStatus(resource) {
      if (this.variable.repeats) {
        const statusList = this.variable.repeats.map((repeatedVariable) => {
          const resourceMapping = this.resourceMappings.find((mapping) => {
            return (
              mapping.toVariable.name === repeatedVariable.name &&
              mapping.fromTable.dataDictionary.resource.pid === resource.pid
            );
          });

          return resourceMapping ? resourceMapping.match.name : "zna";
        });

        if (
          statusList.includes("complete") &&
          !statusList.includes("zna") &&
          statusList.includes("partial")
        ) {
          return "complete";
        } else if (
          statusList.includes("partial") ||
          (statusList.includes("complete") && statusList.includes("zna"))
        ) {
          return "partial";
        } else {
          return "unmapped";
        }
      } else {
        const resourceMapping = this.resourceMappings.find((mapping) => {
          return mapping.fromTable.dataDictionary.resource.pid === resource.pid;
        });

        if (!resourceMapping) {
          return "danger";
        }

        switch (resourceMapping.match.name) {
          case "na":
            return "unmapped";
          case "partial":
            return "complete";
          case "complete":
            return "complete";
          default:
            return "unmapped";
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
.table-label {
  font-size: 0.8rem;
}

.table-bordered th,
.table-bordered td {
  border: 1px solid #6c757d;
}
</style>
