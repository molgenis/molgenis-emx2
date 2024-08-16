<template>
  <div class="mt-4" v-if="attribute.length">
    <div>
      <label class="font-weight-bold mr-3">Split by:</label>
      <div class="d-inline-flex justify-content-around w-50">
        <label>
          <input
            type="checkbox"
            @change="(e) => toggleColumn(e, 'sample_type')"
            :checked="columnChecked('sample_type')"
          />
          Material type
        </label>
        <label>
          <input
            type="checkbox"
            @change="(e) => toggleColumn(e, 'sex')"
            :checked="columnChecked('sex')"
          />
          Sex
        </label>
        <label>
          <input
            type="checkbox"
            @change="(e) => toggleColumn(e, 'age_range')"
            :checked="columnChecked('age_range')"
          />
          Age range
        </label>
        <label>
          <input
            type="checkbox"
            @change="(e) => toggleColumn(e, 'disease')"
            :checked="columnChecked('disease')"
          />
          Disease codes
        </label>
      </div>
    </div>
    <div v-if="splitByColumn.length < 4" class="alert alert-dark" role="alert">
      Because of the adopted method of data creation and collection the number
      of donors presented in the table below should not be added as it may give
      the wrong sums.
    </div>
    <Pagination v-model="currentPage" :count="facts.length" :limit="100" />
    <table class="table border w-100" :key="tableVersion">
      <thead>
        <tr class="facts-header bg-secondary text-white">
          <th @click="sort('sample_type')">
            Material type
            <span
              v-if="sortColumn === 'sample_type'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th @click="sort('sex')">
            Sex
            <span
              v-if="sortColumn === 'sex'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th @click="sort('age_range')">
            Age range
            <span
              v-if="sortColumn === 'age_range'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th @click="sort('disease')">
            Disease codes
            <span
              v-if="sortColumn === 'disease'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th @click="sort('number_of_donors')">
            #Donors
            <span
              v-if="sortColumn === 'number_of_donors'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
          <th @click="sort('number_of_samples')">
            #Samples
            <span
              v-if="sortColumn === 'number_of_samples'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            />
          </th>
        </tr>
        <tr class="filter-bar">
          <th>
            <select
              @change="filter('sample_type', $event)"
              v-model="sampleFilter"
              class="w-100"
            >
              <option value="all">All</option>
              <option
                v-for="material of materialtypeOptions"
                :key="material"
                :value="material"
              >
                {{ material }}
              </option>
            </select>
          </th>
          <th>
            <select @change="filter('sex', $event)" v-model="sexFilter">
              <option value="all">All</option>
              <option v-for="sex of sexOptions" :key="sex" :value="sex">
                {{ sex }}
              </option>
            </select>
          </th>

          <th>
            <select @change="filter('age_range', $event)" v-model="ageFilter">
              <option value="all">All</option>
              <option
                v-for="ageRange of ageRangeOptions"
                :key="ageRange"
                :value="ageRange"
              >
                {{ ageRange }}
              </option>
            </select>
          </th>
          <th>
            <select @change="filter('disease', $event)" v-model="diseaseFilter">
              <option value="all">All</option>
              <option
                v-for="disease of diseaseOptions"
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
      <tbody>
        <template v-for="fact of factsTable" :key="fact.id">
          <tr>
            <th scope="row" class="pr-1 align-top">
              {{ fact.sample_type }}
            </th>
            <td>{{ fact.sex }}</td>
            <td>{{ fact.age_range }}</td>
            <td :title="fact.disease_name">
              {{ fact.disease }}
            </td>
            <td>{{ fact.number_of_donors }}</td>
            <td>{{ fact.number_of_samples }}</td>
          </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import * as _ from "lodash";
//@ts-ignore
import { Pagination } from "molgenis-components";
import { computed, onMounted, ref } from "vue";

const { attribute } = defineProps<{ attribute: any[] }>();

const currentPage = ref(1);
const facts = ref<Record<string, any>[]>([]);
const sortColumn = ref("");
const sortAsc = ref(false);
const tableVersion = ref(0);
const filters = ref<Record<string, any>[]>([]);
const sampleFilter = ref("all");
const sexFilter = ref("all");
const ageFilter = ref("all");
const diseaseFilter = ref("all");
const splitByColumn = ref<string[]>([
  "sample_type",
  "sex",
  "age_range",
  "disease",
]);
const splittableColumns = ref<string[]>([
  "sample_type",
  "sex",
  "age_range",
  "disease",
  "number_of_samples",
]);
const factsProperties = ref<string[]>([
  "sample_type.label",
  "sex.label",
  "age_range.label",
  "disease.label",
  "disease.name",
  "number_of_samples",
  "number_of_donors",
]);
const factProperties = ref<Record<string, any>>({});

onMounted(() => {
  facts.value = getFactsData();
  factProperties.value = getFactProperties();
});

const materialtypeOptions = computed(() => {
  return factProperties.value?.materialtypeOptions || [];
});

const sexOptions = computed(() => {
  return factProperties.value?.sexOptions || [];
});

const ageRangeOptions = computed(() => {
  return factProperties.value?.ageRangeOptions || [];
});

const diseaseOptions = computed(() => {
  return factProperties.value?.diseaseOptions || [];
});

const columnChecked = computed(() => {
  return (column: string) => splitByColumn.value.includes(column);
});

const factsTable = computed(() => {
  const filteredFacts = getFilteredFacts(facts.value);

  const firstIndex = 0 + (currentPage.value - 1) * 100;
  const potentialLastIndex = 99 + (currentPage.value - 1) * 100;
  const lastIndex =
    potentialLastIndex > facts.value.length
      ? facts.value.length
      : potentialLastIndex;

  return filteredFacts.slice(firstIndex, lastIndex);
});

function getFilteredFacts(facts: Record<string, any>[]) {
  if (filters.value.length === 0) {
    return facts;
  }
  const filteredFacts: Record<string, any>[] = [];

  const lastFilterIndex = filters.value.length - 1;

  for (const fact of facts) {
    filters.value.forEach((filter, index) => {
      const propertyValue = getValue(fact, filter.column);
      const matchesAllFilters =
        (!propertyValue && filter.value !== "Unknown") ||
        propertyValue !== filter.value;

      if (!matchesAllFilters) {
        // break;
      } else if (!propertyValue && filter.value === "Unknown") {
        filteredFacts.push(fact);
      } else if (index === lastFilterIndex) {
        filteredFacts.push(fact);
      }
    });
  }
  return filteredFacts;
}

function getFactProperties() {
  const splitFacts: Record<string, string[]> = facts.value.reduce(
    (accum, fact) => {
      if (fact.sample_type) accum.materialtypeOptions.push(fact.sample_type);
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
      materialtypeOptions: [] as string[],
      sexOptions: [] as string[],
      ageRangeOptions: [] as string[],
      diseaseOptions: [] as string[],
    }
  );

  const uniqSplitFacts = {
    materialtypeOptions: _.uniq(splitFacts.materialtypeOptions),
    sexOptions: _.uniq(splitFacts.sexOptions),
    ageRangeOptions: _.uniq(splitFacts.ageRangeOptions),
    diseaseOptions: _.uniq(splitFacts.diseaseOptions),
  };

  return {
    materialtypeOptions: _.sortBy(
      uniqSplitFacts.materialtypeOptions,
      (a: string, b: string) => a.localeCompare(b)
    ),
    sexOptions: _.sortBy(uniqSplitFacts.sexOptions, (a: string, b: string) =>
      a.localeCompare(b)
    ),
    ageRangeOptions: _.sortBy(
      uniqSplitFacts.ageRangeOptions,
      (a: string, b: string) => a.localeCompare(b)
    ),
    diseaseOptions: _.sortBy(
      uniqSplitFacts.diseaseOptions,
      (a: string, b: string) => a.localeCompare(b)
    ),
  };
}

function toggleColumn(e: any, columnName: string) {
  if (e.target.checked) {
    splitByColumn.value.push(columnName);
  } else {
    const newArray = splitByColumn.value.filter((sbc) => sbc !== columnName);
    splitByColumn.value = newArray;
  }
  sampleFilter.value = "all";
  sexFilter.value = "all";
  ageFilter.value = "all";
  diseaseFilter.value = "all";
  filters.value = [];
  collapseRows();
}

function filter(column: string, event: Record<string, any>) {
  const indexToRemove = filters.value.findIndex(
    (fa: Record<string, any>) => fa.column === column
  );

  if (indexToRemove > -1) {
    filters.value.splice(indexToRemove, 1);
  }

  if (event.target.value !== "all") {
    filters.value.push({ column, value: event.target.value });
  }
}

function sort(column: string) {
  /** user clicked again */
  if (sortColumn.value === column) {
    sortAsc.value = !sortAsc;
  } else {
    sortColumn.value = column;
    sortAsc.value = true;
  }

  let newFacts = hardcopy(facts.value);

  newFacts.sort((factA: Record<string, any>, factB: Record<string, any>) => {
    const factAProperty = getValue(factA, column);
    const factBProperty = getValue(factB, column);

    const factValueA = isNaN(factAProperty)
      ? factAProperty
      : parseInt(factAProperty);

    const factValueB = isNaN(factBProperty)
      ? factBProperty
      : parseInt(factBProperty);

    if (factValueA > factValueB) {
      return sortAsc ? 1 : -1;
    } else if (factValueA < factValueB) {
      return sortAsc ? -1 : 1;
    }

    return 0;
  });
  facts.value = newFacts;
  /** if we do not key this, then it will break */
  tableVersion.value = tableVersion.value + 1;
}

function getValue(object: Record<string, any>, propertyString: string) {
  const trail = propertyString.split(".");
  const trailLength = trail.length;

  let value;
  let next = object;
  for (let trailIndex = 0; trailIndex < trailLength; trailIndex++) {
    const trailPart = trail[trailIndex];

    if (!next[trailPart]) {
      return value ?? "Unknown";
    } else {
      value = next[trailPart];
      next = next[trailPart];
    }
  }
  return value ?? "Unknown";
}

function collapseRows() {
  if (splitByColumn.value.length === 4) {
    /** no group together selected, so reset the state */
    // copyFactsToComponentState();
    return;
  }

  /** make a copy that we can keep mutating utill we have dealt with all the collapses.
   * order matters!
   */
  const baseFacts = hardcopy(getFactsData());

  const groupedFacts = [];

  const criteriaMet: string[] = [];

  for (const baseFact of baseFacts) {
    if (Object.values(baseFact).includes("Any")) {
      continue;
    }
    const criteria: Record<string, any> = {};

    let newCriteria = "";

    for (const criteriaColumn of splittableColumns.value) {
      if (splitByColumn.value.includes(criteriaColumn)) {
        const critValue = getValue(baseFact, criteriaColumn);
        /** for use to group */
        criteria[criteriaColumn] = critValue;
        /** track which combination of values has been grouped already  */
        newCriteria += critValue;
      }
    }

    if (!criteriaMet.includes(newCriteria)) {
      let critGroup = baseFacts;
      const criteriaKeys = Object.keys(criteria);
      for (const critKey of criteriaKeys) {
        critGroup = critGroup.filter(
          (obj: Record<string, any>) => obj[critKey] === criteria[critKey]
        );
      }
      criteriaMet.push(newCriteria);
      groupedFacts.push(critGroup);
    }
  }

  const collapsedFacts = [];

  for (const factGroup of groupedFacts) {
    let collapsedFact: Record<string, any> = {};
    for (const fact of factGroup) {
      if (Object.values(fact).includes("Any")) {
        continue;
      }
      if (!Object.keys(collapsedFact).length) {
        collapsedFact = fact;
        continue;
      }

      for (const column of splittableColumns.value) {
        if (!splitByColumn.value.includes(column)) {
          const mergedValue = collapsedFact[column];

          if (Array.isArray(mergedValue)) {
            const valueToMerge = fact[column];
            if (Array.isArray(valueToMerge)) {
              collapsedFact[column] = _.uniq(
                collapsedFact[column].concat(fact[column])
              );
            } else {
              if (!collapsedFact[column].includes(fact[column])) {
                collapsedFact[column].push(fact[column]);
              }
            }
          } else {
            if (collapsedFact[column] !== fact[column]) {
              if (isFinite(collapsedFact[column]) && isFinite(fact[column])) {
                collapsedFact[column] = collapsedFact[column] + fact[column];
              } else if (fact[column] !== "Unknown") {
                collapsedFact[column] = [collapsedFact[column], fact[column]];
              }
            } else if (column === "number_of_samples") {
              collapsedFact[column] = 2 * collapsedFact[column];
            }
          }
        }
      }
    }

    collapsedFact.number_of_donors = "Available";
    collapsedFacts.push(collapsedFact);
  }

  collapsedFacts.forEach((fact) => {
    for (const prop in fact) {
      if (Array.isArray(fact[prop])) {
        fact[prop] = fact[prop].join(", ");
      }
    }
  });

  facts.value = collapsedFacts;
}

function getFactsData() {
  const rawFacts = hardcopy(attribute);
  const facts = rawFacts.map((rawFact: Record<string, any>) => {
    const fact: Record<string, any> = {};
    for (const property of factsProperties.value) {
      const key = property.split(".")[0];

      if (!fact[key]) {
        fact[key] = getValue(rawFact, property);
      }
    }
    return fact;
  });

  return facts.filter(hasAFactToShow);
}

function hardcopy(value: any) {
  return JSON.parse(JSON.stringify(value));
}

function hasAFactToShow(fact: Record<string, any>) {
  const hasSamples =
    fact.number_of_samples && parseInt(fact.number_of_samples) !== 0;
  const hasDonors =
    fact.number_of_donors && parseInt(fact.number_of_samples) !== 0;

  return hasSamples || hasDonors || !!fact.sex;
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
