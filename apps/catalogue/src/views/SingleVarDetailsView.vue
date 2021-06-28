<template>
  <div v-if="variable">
    <div class="row">
      <variable-details class="col" :variableDetails="variable" />
    </div>
    <div class="row">
      <div class="col">
        <table class="table table-bordered table-sm">
          <caption>
            Harmonization summary
          </caption>
          <thead>
            <tr>
              <th scope="col"></th>
              <th
                class="rotated-text text-nowrap"
                scope="col"
                v-for="resource in resources"
                :key="resource.acronym"
              >
                <div>
                  <span class="table-label">{{ resource.acronym }}</span>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <th class="table-label text-nowrap" scope="row">
                {{ variable.name }}
              </th>
              <td
                v-for="resource in resources"
                :key="resource.acronym"
                class="colored-grid-cell"
                :class="'table-' + getMatchStatus(variable, resource.acronym)"
              ></td>
            </tr>
            <tr
              v-for="repeatedVariable in variable.repeats"
              :key="repeatedVariable.name"
            >
              <th class="table-label text-nowrap" scope="row">
                {{ repeatedVariable.name }}
              </th>
              <td
                v-for="resource in resources"
                :key="resource.acronym"
                class="colored-grid-cell"
                :class="
                  'table-' + getMatchStatus(repeatedVariable, resource.acronym)
                "
              ></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script>
import VariableDetails from "../components/VariableDetails.vue";
import { fetchResources } from "../store/repository/resourceRepository";
export default {
  name: "SingleVarDetailsView",
  components: { VariableDetails },
  props: {
    name: String,
    network: String,
    version: String,
    variable: Object,
  },
  data() {
    return {
      resources: null,
    };
  },
  methods: {
    getMatchStatus(variable, resourceName) {
      if (!variable.mappings) {
        return "danger"; // not mapped
      }
      const resourceMapping = variable.mappings.find((mapping) => {
        return mapping.fromRelease.resource.acronym === resourceName;
      });
      if (!resourceMapping) {
        return "danger"; // not mapped
      }
      const match = resourceMapping.match.name;
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
  async created() {
    this.resources = await fetchResources();
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
