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
              :isOpenByDefault="true"
            >
              <label>Gender at birth</label>
              <InputRefList
                id="GenderAtBirth"
                tableId="GenderAtBirth"
                v-model="genderAtBirth"
                refLabel="${name}"
                :multi-select="true"
                @optionsLoaded="genderAtBirthData = $event"
              />
            </Accordion>
            <Accordion
              id="gene-filter"
              title="Filter by gene"
              :isOpenByDefault="true"
            >
              <label>Choose Gene</label>
              <InputRefList
                id="Genes"
                tableId="Genes"
                v-model="genes"
                refLabel="${name}"
                :multi-select="true"
                @optionsLoaded="geneData = $event"
              />
            </Accordion>
          </form>
        </aside>
        <div class="sidebar-main main-beacon-output">
          <h3>Results</h3>
          <div v-if="beaconOutput">
            <DataTable
              tableId="beacon-response"
              :data="beaconResult"
              :columnOrder="['tables', 'status', 'count']"
            />
            <Accordion id="beacon-query" title="View Beacon response">
              <code>
                <pre>{{ beaconOutput }}</pre>
              </code>
            </Accordion>
          </div>
          <div v-else>
            <p>
              A Beacon query has not been created. Build a new query to view the
              results.
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
} from "molgenis-viz";
import { InputRefList } from "molgenis-components";
import { filterData } from "../utils/index";
import axios from "axios";

const genes = ref([]);
const geneData = ref([]); // FIX: will only hold the initial  10 results show on screen
const genderAtBirth = ref([]);
const genderAtBirthData = ref([]);
const beaconOutput = ref(null);
const beaconResult = ref([]);
const error = false;

watch([genes, genderAtBirth], queryBeacon);

async function queryBeacon() {
  const jsQuery = {
    query: {
      filters: [],
    },
  };

  const filterGeneDataValue = filterData(geneData.value, genes.value, ["name"]);
  if (filterGeneDataValue.length > 0) {
    jsQuery.query.filters.push({
      operator: "=",
      id: "NCIT_C16612",
      value: filterGeneDataValue,
    });
  }

  const filterGenderAtBirthDataValue = filterData(
    genderAtBirthData.value,
    genderAtBirth.value,
    ["codesystem", "code"]
  );
  if (filterGenderAtBirthDataValue.length > 0) {
    jsQuery.query.filters.push({
      operator: "=",
      id: "NCIT_C28421",
      value: filterGenderAtBirthDataValue,
    });
  }

  axios
    .post("/api/beacon/individuals", JSON.stringify(jsQuery, null, 2))
    .then((response) => {
      const result = response.data;
      beaconOutput.value = result;
      beaconResult.value = [
        {
          tables: result.meta.returnedSchemas
            .map((schema) => schema.entityType)
            .join(", "),
          count: result.responseSummary.numTotalResults
            ? result.responseSummary.numTotalResults
            : "-",
          status:
            result.responseSummary.exists === "true"
              ? "Available"
              : "Unavailable",
        },
      ];
    })
    .catch((err) => (error.value = err));
}
</script>
