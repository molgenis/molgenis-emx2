<template>
  <div class="mt-3">
    <template v-if="variables.length && resources.length">
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
              v-for="resource in resourcesWithoutModels"
              :key="resource.pid"
            >
              <div>
                <span class="table-label">{{ resource.pid }}</span>
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          <template v-for="variable in variablePage">
            <harmonization-row
              :key="variable.name"
              :variable="variable"
              :resources="resourcesWithoutModels"
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
    <div v-else>
      <Spinner />
    </div>
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
    ...mapGetters(["resources", "variables"]),
    variablePage() {
      return this.variables.slice(0, this.pageSize);
    },
    resourcesWithoutModels() {
      return this.resources.filter(
        (r) =>
          !r.mg_tableclass.endsWith("Models") &&
          !r.mg_tableclass.endsWith("Networks")
      );
    },
  },
  methods: {
    ...mapActions(["fetchResources"]),
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
    await this.fetchResources();
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
