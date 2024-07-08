<template>
  <Page>
    <PageHeader
      title="GDI Local Portal"
      subtitle="Search with Beacon"
      imageSrc="bkg-beacon.jpg"
      titlePositionX="center"
      height="large"
    />
    <PageSection
      width="large"
      class="bg-gray-050"
      :horizontalPadding="2"
      aria-labelledby="datasets-title"
    >
      <div class="sidebar-layout">
        <aside class="sidebar-menu">
          <h3>Build a query</h3>
          <p>
            Create a new beacon query by applying one or more of the following
            filters.
          </p>
          <form>
            <Accordion
              id="sex-at-birth-filter"
              title="Filter by gender at birth"
            >
              <label>Gender at birth</label>
              <InputRefList
                id="GenderAtBirth"
                tableId="GenderAtBirth"
                v-model="genderFilters" 
                refLabel="${name}"
                @optionsLoaded="genderData = $event"
              />
            </Accordion>
            <Accordion id="gene-filter" title="Filter by gene">
              <label>Choose Gene</label>
              <InputRefList
                id="Genes"
                tableId="Genes"
                v-model="geneFilters"
                refLabel="${name}"
                @optionsLoaded="geneData = $event"
              />
            </Accordion>
          </form>
        </aside>
        <div class="sidebar-main main-beacon-output">
          <h3>Results</h3>
          <LoadingScreen v-if="loading" class="beacon-search-loading" />
          <MessageBox v-if="error" type="error">
            <p>{{ error }}</p>
          </MessageBox>
          <div v-if="!loading && beaconOutput">
            <DataTable
              tableId="beacon-response"
              :data="beaconResult"
              :columnOrder="['schema', 'table', 'status', 'count']"
              :renderHtml="true"
            />
            <Accordion id="beacon-query" title="View Beacon response">
              <code>
                <pre>{{ beaconOutput }}</pre>
              </code>
            </Accordion>
          </div>
          <div v-if="!loading && !beaconOutput">
            <p>
              To get started, apply one or more filters. Results will appear in
              this space when a selection is made or filters change.
            </p>
          </div>
        </div>
      </div>
    </PageSection>
  </Page>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import {
  Page,
  PageHeader,
  PageSection,
  Accordion,
  DataTable,
  LoadingScreen,
  MessageBox,
} from "molgenis-viz";
import { InputRefList } from "molgenis-components";
import { filterData, transformBeaconResultSets } from "../utils/index";
import type { BeaconQueryIF } from "../interfaces";
import axios from "axios";

const loading = ref<boolean>(false);
const error = ref<boolean>(false);
const geneFilters = ref([]);
const geneData = ref([]); // FIX: will only hold the initial  10 results show on screen
const genderFilters = ref([]);
const genderData = ref([]);
const beaconOutput = ref(null);
const beaconResult = ref([]);

const jsQuery = ref<BeaconQueryIF>({ query: { filters: [] } });

function prepareJsQuery() {
  jsQuery.value.query.filters = [];
  if (geneFilters.value.length > 0) {
    const geneCodeFilters = filterData(geneData.value, geneFilters.value, [
      "name",
    ]);
    if (geneCodeFilters.length > 0) {
      jsQuery.value.query.filters.push({
        operator: "=",
        id: "NCIT:C16612",
        value: geneCodeFilters,
      });
    }
  }

  if (genderFilters.value.length > 0) {
    const genderCodeFilters = filterData(
      genderData.value,
      genderFilters.value,
      ["codesystem", "code"]
    );
    if (genderCodeFilters.length > 0) {
      jsQuery.value.query.filters.push({
        operator: "=",
        id: "NCIT:C28421",
        value: genderCodeFilters,
      });
    }
  }
}

async function queryBeacon() {
  axios
    .post("/api/beacon/individuals", JSON.stringify(jsQuery.value, null, 2))
    .then((response) => {
      beaconOutput.value = response.data;
      const data = response.data;
      const resultSets = data.response.resultSets;
      beaconResult.value = transformBeaconResultSets(resultSets);
    })
    .catch((err) => {
      error.value = `${err.message} (${err.code})` 
    })
    .finally(() => (loading.value = false));
}

watch([geneFilters, genderFilters], async () => {
  loading.value = true;
  prepareJsQuery();
  await queryBeacon();
});
</script>
