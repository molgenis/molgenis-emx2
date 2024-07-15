<template>
  <Page id="demo-dashboard">
    <PageSection
      class="bkg-light"
      :verticalPadding="2"
      v-if="!loading && error"
    >
      <MessageBox type="error">
        <p>{{ error }}</p>
      </MessageBox>
    </PageSection>
    <Dashboard v-else-if="!loading && !error">
      <h2>Dashboard</h2>
      <button id="reset-filters" @click="resetFilters">
        <span>reset filters</span>
      </button>
      <DashboardRow :columns="2">
        <DashboardChart>
          <DataTable
            tableId="institutions-by-location"
            :data="organisationsInfo"
            caption="Top 10 cities by total number of organisations"
            :columnOrder="['city', 'country', 'organisations']"
            :renderHtml="true"
            @row-clicked="updateClicked"
          />
        </DashboardChart>
        <DashboardChart>
          <BarChart
            chartId="column-organisations-by-types"
            title="Organisations by type"
            description="The following chart shows the number of organisations by type."
            :chartData="organisationsByType"
            xvar="count"
            yvar="type"
            y-axis-line-breaker=" "
            :chartMargins="{ left: 120, top: 10, right: 40, bottom: 60 }"
            :barPaddingInner="0.25"
            :barPaddingOuter="0.25"
            :enable-clicks="false"
          />
        </DashboardChart>
      </DashboardRow>
    </Dashboard>
  </Page>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import { gql, request } from "graphql-request";
import { rollups, flatRollup } from "d3";
const d3 = { rollups, flatRollup };

import Page from "../components/layouts/Page.vue";
import PageSection from "../components/layouts/PageSection.vue";
import Dashboard from "../components/layouts/Dashboard.vue";
import DashboardRow from "../components/layouts/DashboardRow.vue";
import DashboardChart from "../components/layouts/DashboardChart.vue";
import ColumnChart from "../components/viz/ColumnChart.vue";
import BarChart from "../components/viz/BarChart.vue";
import DataTable from "../components/viz/DataTable.vue";
import MessageBox from "../components/display/MessageBox.vue";

let loading = ref<boolean>(true);
let error = ref<boolean>(false);
let selection = ref<object>({});
let organisations = ref<Array>([]);
let organisationsByType = ref<Array>([]);
let organisationsInfo = ref<Array>([]);
let queryFilters = ref<object>({ filter: {} });

function onClick() {
  console.log("clicked");
}

async function getData() {
  const query = gql`
    query ($filters: OrganisationsFilter) {
      Organisations(filter: $filters) {
        name
        organisationType
        city
        country
      }
    }
  `;
  const variables = { filters: queryFilters.value.filter };
  const response = await request("../api/graphql", query, variables);
  organisations.value = response.Organisations;

  organisationsByType.value = d3
    .rollups(
      organisations.value,
      (row) => row.length,
      (row) => row.organisationType + " Organisations"
    )
    .map((group) => new Object({ type: group[0], count: group[1] }))
    .sort((a, b) => (a.type < b.type ? -1 : 1));

  organisationsInfo.value = d3
    .flatRollup(
      organisations.value,
      (row) => row.length,
      (row) => row.city,
      (row) => row.country
    )
    .map((arr) => {
      return { city: arr[0], country: arr[1], organisations: arr[2] };
    })
    .sort((a, b) => (a.organisations < b.organisations ? 1 : -1))
    .slice(0, 10);
}

function updateClicked(value) {
  selection.value = value;
  updateFilters();
  getData();
}

function updateFilters() {
  const ignoreColumns = ["organisations"];
  const query = {};
  Object.keys(selection.value).forEach((key) => {
    if (ignoreColumns.indexOf(key) === -1) {
      query[key] = { equals: selection.value[key] };
    }
  });
  queryFilters.value.filter = query;
}

function resetFilters() {
  queryFilters.value.filter = {};
  selection.value = {};
  getData();
}

onMounted(() => {
  getData()
    .catch((err) => (error.value = err))
    .finally(() => (loading.value = false));
});
</script>

<style lang="scss">
#reset-filters {
  width: 150px;
  background: none;
  border: none;
  background-color: $blue-100;
  color: $blue-900;
  border-radius: 8px;
}

#institutions-by-location {
  thead {
    th {
      font-size: 0.8rem;
    }
  }
  tbody {
    td {
      font-size: 1rem;
      padding: 0.6em 0.4em;
    }
  }
}
</style>
