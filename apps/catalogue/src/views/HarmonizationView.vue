<template>
  <div class="mt-3">
    <template v-if="variables?.length">
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
              v-for="cohort in cohorts"
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
            <harmonization-row :variable="variable" :resources="cohorts" />
          </template>
        </tbody>
      </table>
    </template>
  </div>
</template>

<script>
import HarmonizationRow from "./HarmonizationRow.vue";

export default {
  name: "HarmonizationView",
  components: { HarmonizationRow },
  props: {
    variables: Array,
    cohorts: Array,
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
