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
        <template v-for="variable in variables">
          <harmonization-row
            :key="variable.name"
            :variable="variable"
            :cohorts="cohorts"
          />
        </template>
      </tbody>
    </table>
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import HarmonizationRow from "./HarmonizationRow.vue";

export default {
  name: "HarmonizationView",
  components: { HarmonizationRow },
  computed: {
    ...mapGetters(["cohorts", "variables"]),
  },
  methods: {
    ...mapActions(["fetchCohorts"])
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
