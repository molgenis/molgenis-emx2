<template>
  <div class="mt-3">
    <template v-if="variables.length && cohorts.length">
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
          <template v-for="variable in variablePage">
            <harmonization-row
              :key="variable.name"
              :variable="variable"
              :cohorts="cohorts"
            />
          </template>
        </tbody>
      </table>
      <p v-if="pageSize < variables.length">
        <span class="mg-btn-align text-muted">
          {{ pageSize }} of {{ variables.length }} matching variables</span
        >
        <button
          class="btn btn-link"
          v-if="variables.length"
          @click="fetchNextPage"
        >
          Load more
        </button>
      </p>
    </template>
    <div v-else><Spinner /></div>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import HarmonizationRow from "./HarmonizationRow.vue";
import { Spinner } from "@mswertz/emx2-styleguide";

const INITIAL_PAGE_SIZE = 10;

export default {
  name: "HarmonizationView",
  components: { HarmonizationRow, Spinner },
  data() {
    return {
      pageSize: INITIAL_PAGE_SIZE,
    };
  },
  computed: {
    ...mapGetters(["cohorts", "variables"]),
    variablePage() {
      return this.variables.slice(0, this.pageSize);
    },
  },
  methods: {
    ...mapActions(["fetchCohorts"]),
    fetchNextPage() {
      this.pageSize += 10;
    },
  },
  watch: {
    variables() {
      this.pageSize = INITIAL_PAGE_SIZE;
    },
  },
  async mounted() {
    await this.fetchCohorts();
  },
};
</script>

<style scoped>
th.rotated-text {
  height: 13rem;
  padding: 0;
}
th.rotated-text > div {
  transform: rotate(270deg);
  width: 1.8rem;
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

.mg-btn-align {
  vertical-align: middle;
}
</style>
