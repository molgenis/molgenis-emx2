<template>
  <Page>
    <PageHeader
      title="GDI Local Portal"
      subtitle="Search with Beacon"
      imageSrc="img/bkg-beacon.jpg"
      titlePositionX="center"
      height="large"
    />
    <PageSection
      width="large"
      class="bg-gray-050"
      :horizontalPadding="1"
      aria-labelledby="datasets-title"
    >
      <div class="sidebar-layout">
        <aside class="sidebar-menu">
          <h3>Build a query</h3>
          <p>
            Create a new beacon query by applying one or more of the following
            filters.
          </p>
          <form @submit.prevent>
            <Accordion
              id="sex-at-birth-filter"
              title="Filter by gender at birth"
              :isOpenByDefault="true"
            >
              <CheckBoxSearch
                id="gender-at-birth-input"
                label="Search for gender at birth"
                tableId="GenderAtBirth"
                :columns="['name', 'codesystem', 'code']"
                id-column="code"
                value-column="name"
                label-column="name"
                @ref-data-loaded="genderData = $event"
                @change="genderFilters = $event"
              />
            </Accordion>
            <Accordion id="gene-filter" title="Filter by gene">
              <CheckBoxSearch
                id="genes-list"
                label="Search for a gene"
                tableId="Genes"
                :columns="['name']"
                id-column="name"
                value-column="name"
                label-column="name"
                @ref-data-loaded="geneData = $event"
                @change="geneFilters = $event"
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
            <p>
              {{ beaconResultHits }} result{{
                beaconResultHits > 1 || beaconResultHits === 0 ? "s" : null
              }}
            </p>
            <DataTable
              tableId="beacon-response"
              :data="beaconResult"
              :columnOrder="['schema', 'table', 'status', 'count']"
              :renderHtml="true"
            />
            <Accordion id="beacon-query" title="View Beacon information">
              <h4>Query</h4>
              <code>
                <pre>{{ jsQuery }}</pre>
              </code>
              <h4>Response</h4>
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

// @ts-ignore
// prettier-ignore
import { Page, PageHeader, PageSection, Accordion, DataTable, LoadingScreen, MessageBox, } from "molgenis-viz";

import CheckBoxSearch from "../components/CheckBoxSearch.vue";

import { filterData, transformBeaconResultSets } from "../utils/beacon";
import type {
  BeaconQueryIF,
  ApiResponseIF,
  OntologyDataIF,
  BeaconOutputIF,
  BeaconResultsIF,
} from "../interfaces/beacon";
import axios from "axios";

const loading = ref<boolean>(false);
const error = ref<string | boolean>(false);

const geneData = ref<OntologyDataIF[]>([]);
const genderData = ref<OntologyDataIF[]>([]);

const geneFilters = ref<string[]>([]);
const genderFilters = ref<string[]>([]);

const beaconOutput = ref<BeaconOutputIF>();
const beaconResultHits = ref<number>(0);
const beaconResult = ref<BeaconResultsIF[]>([]);
const jsQuery = ref<BeaconQueryIF>({
  query: { filters: [] },
});

function prepareBeaconQuery() {
  jsQuery.value.query.filters = [];

  if (genderFilters.value.length) {
    const genderTermsFiltered = filterData(
      genderData.value,
      genderFilters.value,
      ["codesystem", "code"]
    );

    if (genderTermsFiltered.length) {
      jsQuery.value.query.filters.push({
        operator: "=",
        id: "NCIT:C28421",
        value: genderTermsFiltered,
      });
    }
  }

  if (geneFilters.value.length) {
    const geneTermsFiltered = filterData(geneData.value, geneFilters.value, [
      "name",
    ]);

    if (geneTermsFiltered.length) {
      jsQuery.value.query.filters.push({
        operator: "=",
        id: "edam:data_2295",
        value: geneTermsFiltered,
      });
    }
  }
}

async function queryBeacon() {
  axios
    .post("/api/beacon/individuals", JSON.stringify(jsQuery.value, null, 2))
    .then((response: ApiResponseIF) => {
      beaconOutput.value = response.data;
      const data = response.data;
      const resultSets = data.response.resultSets;
      beaconResult.value = transformBeaconResultSets(resultSets);
      beaconResultHits.value =
        beaconOutput.value.responseSummary.numTotalResults;
    })
    .catch((err) => {
      error.value = `${err.message} (${err.code})`;
    })
    .finally(() => (loading.value = false));
}

watch([geneFilters, genderFilters], async () => {
  error.value = false;
  loading.value = true;
  prepareBeaconQuery();
  await queryBeacon();
});
</script>
