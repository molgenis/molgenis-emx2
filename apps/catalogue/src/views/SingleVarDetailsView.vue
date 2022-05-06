<template>
  <div v-if="variable">
    <div class="row">
      <variable-details class="col" :variableDetails="variable" />
    </div>
    <div class="row">
      <div class="col">
        <table class="table table-bordered table-sm">
          <caption>
            <h5>Harmonization status</h5>
            <span
              ><span class="table-success"
                ><i class="fa fa-fw fa-check"
              /></span>
              = completed,
            </span>
            <span
              ><span class="table-warning"
                ><i class="fa fa-fw fa-percent"
              /></span>
              = partial,
            </span>
          </caption>
          <thead>
            <tr>
              <th scope="col"></th>
              <th
                class="rotated-text text-nowrap"
                scope="col"
                v-for="resource in resources"
                :key="resource.pid"
              >
                <div>
                  <span class="table-label">{{ resource.pid }}</span>
                </div>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <th class="table-label text-nowrap" scope="row">
                {{ variable.name }}
              </th>
              <harmonization-cell
                v-for="resource in resources"
                :key="resource.pid"
                :status="getMatchStatus(variable, resource.pid)"
              />
            </tr>
            <tr
              v-for="repeatedVariable in variable.repeats"
              :key="repeatedVariable.name"
            >
              <th class="table-label text-nowrap" scope="row">
                {{ repeatedVariable.name }}
              </th>

              <harmonization-cell
                v-for="resource in resources"
                :key="resource.pid"
                :status="getMatchStatus(repeatedVariable, resource.pid)"
              />
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
import HarmonizationCell from "../components/harmonization/HarmonizationCell";

export default {
  name: "SingleVarDetailsView",
  components: { VariableDetails, HarmonizationCell },
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
        return "unmapped"; // not mapped
      }
      const resourceMapping = variable.mappings.find((mapping) => {
        return mapping.fromDataDictionary.resource.pid === resourceName;
      });
      if (!resourceMapping) {
        return "unmapped"; // not mapped
      }
      const match = resourceMapping.match.name;
      switch (match) {
        case "na":
          return "unmapped";
        case "partial":
          return "partial";
        case "complete":
          return "complete";
        default:
          return "unmapped";
      }
    },
  },
  async created() {
    this.resources = await fetchResources();
  },
};
</script>

<style scoped>
caption {
  caption-side: top;
}

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
