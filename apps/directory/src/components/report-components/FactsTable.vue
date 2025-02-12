<template>
  <div class="mt-4" v-if="attribute.length">
    <Pagination
      v-model="currentPage"
      :count="filteredFacts.length"
      :limit="100"
    />
    <div>
      <label class="font-weight-bold mr-3">Split by:</label>
      <div class="d-inline-flex justify-content-around w-50">
        <label>
          <input
            type="checkbox"
            @change="(event) => toggleColumn(event, 'sample_type')"
            :checked="columnChecked('sample_type')"
          />
          Material type
        </label>
        <label>
          <input
            type="checkbox"
            @change="(event) => toggleColumn(event, 'sex')"
            :checked="columnChecked('sex')"
          />
          Sex
        </label>
        <label>
          <input
            type="checkbox"
            @change="(event) => toggleColumn(event, 'age_range')"
            :checked="columnChecked('age_range')"
          />
          Age range
        </label>
        <label>
          <input
            type="checkbox"
            @change="(event) => toggleColumn(event, 'disease')"
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
            <select v-model="filters['sample_type']" class="w-100">
              <option :value="ALL">All</option>
              <option
                v-for="material of factProperties.materialtypeOptions"
                :key="material"
                :value="material"
              >
                {{ material }}
              </option>
            </select>
          </th>
          <th>
            <select v-model="filters['sex']">
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

          <th>
            <select v-model="filters['age_range']">
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
          <th>
            <select v-model="filters['disease']">
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
      <tbody>
        <template v-for="fact of factsTable" :key="fact.id">
          <tr>
            <th scope="row" class="pr-1 align-top">
              {{ fact.sample_type }}
            </th>
            <td>{{ fact.sex }}</td>
            <td>{{ fact.age_range }}</td>
            <td>{{ fact.disease }}</td>
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
import { computed, onMounted, ref, watch } from "vue";

const { attribute } = defineProps<{ attribute: any[] }>();

const ALL = "all";
const UNKNOWN = "Unknown";
const NOFILTERS = { sample_type: ALL, sex: ALL, age_range: ALL, disease: ALL };
const FACTSPROPERTIES = [
  "sample_type.label",
  "sex.label",
  "age_range.label",
  "disease.label",
  "disease.name",
  "number_of_samples",
  "number_of_donors",
];
const SPLITTABLE_COLUMNS = [
  "sample_type",
  "sex",
  "age_range",
  "disease",
  "number_of_samples",
];

const currentPage = ref(1);
const facts = ref<Record<string, any>[]>([]);
const sortColumn = ref("");
const sortAsc = ref(false);
const tableVersion = ref(0);
const filters = ref<Record<string, any>>(NOFILTERS);
const splitByColumn = ref<string[]>([
  "sample_type",
  "sex",
  "age_range",
  "disease",
]);

const factProperties = ref<Record<string, any>>({});

let baseFacts: Record<string, any>[] = [];

onMounted(() => {
  baseFacts = getBaseFacts(attribute);
  facts.value = baseFacts;
  factProperties.value = getFactProperties();
});

const columnChecked = computed(() => {
  return (column: string) => splitByColumn.value.includes(column);
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
    hardcopy(facts)
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
    materialtypeOptions: _.sortBy(uniqSplitFacts.materialtypeOptions),
    sexOptions: _.sortBy(uniqSplitFacts.sexOptions),
    ageRangeOptions: _.sortBy(uniqSplitFacts.ageRangeOptions),
    diseaseOptions: _.sortBy(uniqSplitFacts.diseaseOptions),
  };
}

function toggleColumn(event: Record<string, any>, columnName: string) {
  if (event.target.checked) {
    splitByColumn.value.push(columnName);
  } else {
    const newArray = splitByColumn.value.filter((sbc) => sbc !== columnName);
    splitByColumn.value = newArray;
  }
  filters.value = NOFILTERS;
  collapseRows();
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
  const rawFacts = hardcopy(attribute);
  const facts = rawFacts.map((rawFact: Record<string, any>) => {
    const fact: Record<string, any> = {};
    for (const property of FACTSPROPERTIES) {
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
    fact.number_of_donors && parseInt(fact.number_of_donors) !== 0;

  return hasSamples || hasDonors || !!fact.sex;
}

function collapseRows() {
  if (splitByColumn.value.length === 4) {
    /** no group together selected, so reset the state */
    facts.value = baseFacts;
    return;
  }

  /** make a copy that we can keep mutating utill we have dealt with all the collapses.
   * order matters!
   */
  const baseFactsCopy = hardcopy(baseFacts);

  const groupedFacts = [];

  const criteriaMet: string[] = [];

  for (const baseFact of baseFactsCopy) {
    if (Object.values(baseFact).includes("Any")) {
      continue;
    }
    const criteria: Record<string, any> = {};

    let newCriteria = "";

    for (const criteriaColumn of SPLITTABLE_COLUMNS) {
      if (splitByColumn.value.includes(criteriaColumn)) {
        const critValue = getValue(baseFact, criteriaColumn);
        /** for use to group */
        criteria[criteriaColumn] = critValue;
        /** track which combination of values has been grouped already  */
        newCriteria += critValue;
      }
    }

    if (!criteriaMet.includes(newCriteria)) {
      let critGroup = baseFactsCopy;
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

      for (const column of SPLITTABLE_COLUMNS) {
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
              } else if (fact[column] !== UNKNOWN) {
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
