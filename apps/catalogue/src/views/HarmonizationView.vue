<template>
  <div class="mt-3">
    <template v-if="variables.length && cohorts.length">
      <table class="table table-bordered table-sm">
        <caption>
          <span
            ><span class="table-success"><i class="fa fa-fw fa-check" /></span>
            = completed,
          </span>
          <span
            ><span class="table-light"><i class="fa fa-fw fa-percent" /></span>
            = partially harmonized</span
          >
        </caption>
        <thead>
          <tr>
            <th scope="col"></th>
            <th
              class="rotated-text text-nowrap"
              scope="col"
              v-for="cohort in cohortsInThisNetwork"
              :key="cohort.id"
            >
              <div>
                <span class="table-label">{{ cohort.id }}</span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <template v-for="variable in variables" :key="variable.name">
            <harmonization-row
              :variable="variable"
              :resources="cohortsInThisNetwork"
            />
          </template>
        </tbody>
      </table>
      <p v-if="isLoading" class="text-center font-italic pt-3">
        <Spinner />
        Fetching variable data..
      </p>
      <button
        class="btn btn-link mt-2 mb-3"
        v-else-if="showMoreVisible"
        @click="fetchAdditionalVariables"
      >
        Show more variables
      </button>
    </template>
  </div>
</template>

<script>
import { mapGetters, mapActions, mapState } from "vuex";
import HarmonizationRow from "./HarmonizationRow.vue";
import { Spinner } from "molgenis-components";

export default {
  name: "HarmonizationView",
  components: { HarmonizationRow, Spinner },
  props: {
    network: String,
  },
  computed: {
    ...mapGetters(["cohorts", "variables", "variableCount"]),
    ...mapState(["isLoading"]),
    showMoreVisible() {
      return this.variables.length < this.variableCount;
    },
    cohortsInThisNetwork() {
      return this.cohorts.filter(
        (c) =>
          this.network === null ||
          c.networks?.some((n) => {
            return n.id === this.network;
          })
      );
    },
  },
  methods: {
    ...mapActions(["fetchCohorts", "fetchAdditionalVariables"]),
    async handleVariableDetailsRequest(variable) {
      const result = await this.fetchVariableDetails(variable);
      variable.variableDetails = result;
    },
  },
  async mounted() {
    await this.fetchCohorts();
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

.table-label {
  font-size: 0.8rem;
}

.table-bordered th,
.table-bordered td {
  border: 1px solid #6c757d;
}

.mg-btn-align {
  vertical-align: middle;
}
</style>
