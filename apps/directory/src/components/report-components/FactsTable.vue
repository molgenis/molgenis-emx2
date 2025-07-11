<template>
  <div class="mt-4" v-if="attribute.length">
    <Pagination
      v-model="currentPage"
      :count="filteredFacts.length"
      :limit="100"
    />

    <div>
      <label class="font-weight-bold mr-3">Split by:</label>
      <div class="d-inline-flex justify-content-around">
        <div>
          <select v-model="splitByColumn" @change="toggleColumn">
            <option value="all">All</option>
            <option value="sample_type">Material type</option>
            <option value="sex">Sex</option>
            <option value="age_range">Age range</option>
            <option value="disease">Disease codes</option>
          </select>
        </div>
      </div>
    </div>

    <table class="table border w-100" :key="tableVersion">
      <thead>
        <tr class="facts-header bg-secondary text-white">
          <th v-if="showColumn(SAMPLE_TYPE)" @click="sort(SAMPLE_TYPE)">
            Material type
            <span
              v-if="sortColumn === SAMPLE_TYPE"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th v-if="showColumn(SEX)" @click="sort(SEX)">
            Sex
            <span
              v-if="sortColumn === SEX"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th v-if="showColumn(AGE_RANGE)" @click="sort(AGE_RANGE)">
            Age range
            <span
              v-if="sortColumn === AGE_RANGE"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th v-if="showColumn(DISEASE)" @click="sort(DISEASE)">
            Disease codes
            <span
              v-if="sortColumn === DISEASE"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th @click="sort(NUMBER_OF_DONORS)">
            #Donors
            <span
              v-if="sortColumn === 'number_of_donors'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th @click="sort(NUMBER_OF_SAMPLES)">
            #Samples
            <span
              v-if="sortColumn === NUMBER_OF_SAMPLES"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
        </tr>

        <tr class="filter-bar">
          <th v-if="showColumn(SAMPLE_TYPE)">
            <select v-model="filters[SAMPLE_TYPE]" class="w-100">
              <option :value="ALL">All</option>
              <option
                v-for="material of factProperties.materialTypeOptions"
                :key="material"
                :value="material"
              >
                {{ material }}
              </option>
            </select>
          </th>
          <th v-if="showColumn(SEX)">
            <select v-model="filters[SEX]">
              <option :value="ALL">All</option>
              <option
                v-for="sex of factProperties.sexOptions"
                :key="sex"
                :value="sex"
              >
                {{ sex }}
              </option>
            </select>
          </th>
          <th v-if="showColumn(AGE_RANGE)">
            <select v-model="filters[AGE_RANGE]">
              <option :value="ALL">All</option>
              <option
                v-for="ageRange of factProperties.ageRangeOptions"
                :key="ageRange"
                :value="ageRange"
              >
                {{ ageRange }}
              </option>
            </select>
          </th>
          <th v-if="showColumn(DISEASE)">
            <select v-model="filters[DISEASE]">
              <option :value="ALL">All</option>
              <option
                v-for="disease of factProperties.diseaseOptions"
                :key="disease"
                :value="disease"
              >
                {{ disease }}
              </option>
            </select>
          </th>
          <th />
          <th />
        </tr>
      </thead>

      <tbody v-if="factsTable.length">
        <template v-for="fact of factsTable" :key="fact.id">
          <tr>
            <th
              v-if="showColumn(SAMPLE_TYPE)"
              scope="row"
              class="pr-1 align-top"
            >
              {{ fact.sample_type }}
            </th>
            <td v-if="showColumn(SEX)">{{ fact.sex }}</td>
            <td v-if="showColumn(AGE_RANGE)">{{ fact.age_range }}</td>
            <td v-if="showColumn(DISEASE)">{{ fact.disease }}</td>
            <td>{{ fact.number_of_donors }}</td>
            <td>{{ fact.number_of_samples }}</td>
          </tr>
        </template>
      </tbody>
      <tbody v-else>
        <tr>
          <td colspan="6" class="text-center">
            <strong>No data available for this selection</strong>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import * as _ from "lodash";
//@ts-ignore
import { Pagination } from "molgenis-components";
import { computed, onMounted, ref, watch } from "vue";

const { attribute } = defineProps<{ attribute: any[] }>();

const ALL = "all";
const ANY = "Any";
const UNKNOWN = "Unknown";
const SAMPLE_TYPE = "sample_type";
const SEX = "sex";
const AGE_RANGE = "age_range";
const DISEASE = "disease";
const NUMBER_OF_DONORS = "number_of_donors";
const NUMBER_OF_SAMPLES = "number_of_samples";

const FACT_PROPERTIES = [
  "sample_type.label",
  "sex.label",
  "age_range.label",
  "disease.label",
  "disease.name",
  NUMBER_OF_SAMPLES,
  NUMBER_OF_DONORS,
];
const COLUMN_IDS = [SAMPLE_TYPE, SEX, AGE_RANGE, DISEASE];
const NO_FILTERS = Object.freeze({
  sample_type: ALL,
  sex: ALL,
  age_range: ALL,
  disease: ALL,
});

const currentPage = ref(1);
const facts = ref<Record<string, any>[]>([]);
const sortColumn = ref("");
const sortAsc = ref(false);
const tableVersion = ref(0);
const filters = ref<Record<string, any>>(hardCopy(NO_FILTERS));
const splitByColumn = ref<string>("all");
const factProperties = ref<Record<string, any>>({});

let baseFacts: Record<string, any>[] = [];

onMounted(() => {
  baseFacts = getBaseFacts(attribute);
  collapseRows();
  factProperties.value = getFactProperties();
});

const filteredFacts = computed(() => {
  return getFilteredFacts(facts.value, filters.value);
});

const factsTable = computed(() => {
  const firstIndex = 0 + (currentPage.value - 1) * 100;
  const potentialLastIndex = 99 + (currentPage.value - 1) * 100;
  const lastIndex =
    potentialLastIndex > filteredFacts.value.length
      ? filteredFacts.value.length
      : potentialLastIndex;

  return filteredFacts.value.slice(firstIndex, lastIndex);
});

watch(
  filters.value,
  () => {
    currentPage.value = 1;
  },
  { deep: true }
);

function getFilteredFacts(
  facts: Record<string, any>[],
  filters: Record<string, string>
) {
  return _.reduce(
    filters,
    (accum, filterValue, filterKey) => {
      if (filterValue === ALL) {
        return accum;
      } else {
        return accum.filter((fact: Record<string, any>) => {
          return fact[filterKey] === filterValue;
        });
      }
    },
    hardCopy(facts)
  );
}

function getValue(object: Record<string, any>, propertyString: string) {
  const trail: string[] = propertyString.split(".");
  const trailLength = trail.length;

  let value: any;
  let next: Record<string, any> = object;
  for (let trailIndex = 0; trailIndex < trailLength; trailIndex++) {
    const trailPart = trail[trailIndex];

    if (typeof next[trailPart] === "object") {
      next = next[trailPart];
    } else {
      value = next[trailPart];
    }
  }
  return value ?? UNKNOWN;
}

function getFactProperties() {
  const splitFacts: Record<string, string[]> = facts.value.reduce(
    (accum, fact) => {
      if (fact.sample_type) accum.materialTypeOptions.push(fact.sample_type);
      if (fact.sex) accum.sexOptions.push(fact.sex);
      if (fact.age_range) accum.ageRangeOptions.push(fact.age_range);
      if (fact.disease) {
        if (fact.disease.name) {
          accum.diseaseOptions.push(fact.disease.name);
        } else {
          accum.diseaseOptions.push(fact.disease);
        }
      }
      return accum;
    },
    {
      materialTypeOptions: [] as string[],
      sexOptions: [] as string[],
      ageRangeOptions: [] as string[],
      diseaseOptions: [] as string[],
    }
  );

  const uniqSplitFacts = {
    materialTypeOptions: _.uniq(splitFacts.materialTypeOptions),
    sexOptions: _.uniq(splitFacts.sexOptions),
    ageRangeOptions: _.uniq(splitFacts.ageRangeOptions),
    diseaseOptions: _.uniq(splitFacts.diseaseOptions),
  };

  return {
    materialTypeOptions: _.sortBy(uniqSplitFacts.materialTypeOptions),
    sexOptions: _.sortBy(uniqSplitFacts.sexOptions),
    ageRangeOptions: _.sortBy(uniqSplitFacts.ageRangeOptions),
    diseaseOptions: _.sortBy(uniqSplitFacts.diseaseOptions),
  };
}

function sort(column: string) {
  if (sortColumn.value === column) {
    sortAsc.value = !sortAsc;
  } else {
    sortColumn.value = column;
    sortAsc.value = true;
  }

  const sortedFacts = _.sortBy(facts.value, column);
  facts.value = sortAsc.value ? sortedFacts : _.reverse(sortedFacts);

  /** if we do not key this, then it will break */
  tableVersion.value = tableVersion.value + 1;
}

function getBaseFacts(attribute: Record<string, any>) {
  const rawFacts = hardCopy(attribute);
  const facts = rawFacts.map((rawFact: Record<string, any>) => {
    const fact: Record<string, any> = {};
    for (const property of FACT_PROPERTIES) {
      const key = property.split(".")[0];

      if (!fact[key]) {
        fact[key] = getValue(rawFact, property);
      }
    }
    return fact;
  });

  return facts.filter(hasAFactToShow);
}

function hardCopy(value: any) {
  return JSON.parse(JSON.stringify(value));
}

function hasAFactToShow(fact: Record<string, any>) {
  const hasSamples =
    fact.number_of_samples && parseInt(fact.number_of_samples) !== 0;
  const hasDonors =
    fact.number_of_donors && parseInt(fact.number_of_donors) !== 0;

  return hasSamples || hasDonors || !!fact.sex;
}

function toggleColumn() {
  filters.value = hardCopy(NO_FILTERS);
  collapseRows();
}

function collapseRows() {
  facts.value = COLUMN_IDS.reduce((accum, columnId) => {
    if (splitByColumn.value === ALL) {
      return accum.filter(
        (fact: Record<string, any>) => fact[columnId] !== ANY
      );
    } else {
      return accum.filter((fact: Record<string, any>) => {
        if (splitByColumn.value === columnId) {
          return fact[columnId] !== ANY;
        } else {
          return fact[columnId] === ANY;
        }
      });
    }
  }, hardCopy(baseFacts));
}

function showColumn(columnId: string) {
  return splitByColumn.value === columnId || splitByColumn.value === ALL;
}
</script>

<style scoped>
tr th:not(:first-child),
tr td:not(:first-child) {
  text-align: left;
}

.facts-header th:hover {
  cursor: pointer;
  opacity: 0.8;
}

.facts-header th {
  border-bottom: none;
  white-space: nowrap;
}

.filter-bar th {
  border-top: none;
}
</style>
