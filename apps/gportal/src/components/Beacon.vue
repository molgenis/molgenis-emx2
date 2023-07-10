<template>
  <div class="container-fluid">
    <header class="row">
      <h1>GDI Beacon Portal</h1>
    </header>
    <div class="row">
      <aside class="col-2">
        <form>
          <legend>Refine Beacon Query</legend>
          <label>Gender at birth</label>
          <InputRefList
            id="GenderAtBirth"
            tableName="GenderAtBirth"
            v-model="genderAtBirth"
            refLabel="${name}"
            :multi-select="true"
            @optionsLoaded="genderAtBirthData = $event"
          />
          <label>Choose Gene</label>
          <InputRefList
            id="Genes"
            tableName="Genes"
            v-model="genes"
            refLabel="${name}"
            :multi-select="true"
            @optionsLoaded="geneData = $event"
          />
        </form>
      </aside>
      <div class="col-10">
        <section>
          <h2>Results</h2>
          <div v-if="beaconOutput">
            <table class="table table-striped">
              <thead>
                <tr>
                  <th>Count</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    {{
                      beaconOutput.responseSummary.numTotalResults
                        ? beaconOutput.responseSummary.numTotalResults
                        : "-"
                    }}
                  </td>
                  <td>
                    {{
                      beaconOutput.responseSummary.exists === "true"
                        ? "Exists"
                        : "Not found"
                    }}
                  </td>
                </tr>
              </tbody>
            </table>

            <div class="accordion px-1">
              <h3 class="h3">
                <button
                  id="accordionToggle"
                  type="button"
                  class="btn btn-outline-secondary"
                  @click="accordionOpen = !accordionOpen"
                >
                  {{
                    accordionOpen ? "Hide beacon query" : "Show beacon query"
                  }}
                </button>
              </h3>
              <div class="accordion-content" v-show="accordionOpen">
                <code class="d-block p-4 jumbotron">
                  <pre>{{ beaconOutput }}</pre>
                </code>
              </div>
            </div>
          </div>
          <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// @ts-ignore
import { ButtonAction, InputRefList, MessageError } from "molgenis-components";
import axios from "axios";
import { ref, watch } from "vue";

let genes = ref([]);
let geneData = ref([]); // FIX: will only hold the initial  10 results show on screen
let genderAtBirth = ref([]);
let genderAtBirthData = ref([]);
let beaconOutput = ref(null);
let errorMessage = "";
let accordionOpen = ref(false);

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
      beaconOutput.value = response.data;
    });
}

// filterData
// Filter an array of objects based on inputRefList selection, and then return an array
// of strings based on user-specified property names for use in the Beacon API.
//
// @param data dataset containing one or more rows (array of objects)
// @param filters user selected filters (i.e., from InputRefList)
// @param attribs an array containing one or more column names in order of preference
//
// @examples
// const data = filterData(data=myData, filters=myFilters, attribs=['col1','col2'])
//
// @return array of strings
function filterData(data, filters, attributes): string[] {
  return data
    .filter((row) => {
      return filters.map((filterItem) => filterItem.name).includes(row.name);
    })
    .map((row) => {
      return attributes
        .map((attrib) => {
          if (row.hasOwnProperty(attrib)) {
            return row[attrib];
          }
        })
        .join("_");
    });
}
</script>
