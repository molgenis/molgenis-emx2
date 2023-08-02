<template>
  <div class="mt-4" v-if="attribute && attribute.length">
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
            ></span>
          </th>
          <th @click="sort('sex')">
            Sex
            <span
              v-if="sortColumn === 'sex'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            ></span>
          </th>
          <th @click="sort('age_range')">
            Age range
            <span
              v-if="sortColumn === 'age_range'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            ></span>
          </th>
          <th @click="sort('disease')">
            Disease codes
            <span
              v-if="sortColumn === 'disease'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            ></span>
          </th>
          <th @click="sort('number_of_donors')">
            #Donors
            <span
              v-if="sortColumn === 'number_of_donors'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            ></span>
          </th>
          <th @click="sort('number_of_samples')">
            #Samples
            <span
              v-if="sortColumn === 'number_of_samples'"
              class="fa"
              :class="sortAsc ? 'fa-sort-asc' : 'fa-sort-desc'"
              aria-hidden="true"
            ></span>
          </th>
        </tr>
        <tr class="filter-bar">
          <th>
            <select @change="filter('sample_type', $event)" v-model="sampleFilter" class="w-100">
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
          <th></th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <template v-for="fact of factsTable">
          <tr :key="fact.id" v-if="hasAFactToShow(fact)">
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

<script>
export default {
  props: {
    attribute: {
      type: Array,
    },
  },
  data() {
    return {
      facts: [],
      sortColumn: "",
      sortAsc: false,
      tableVersion: 0,
      filters: [],
      sampleFilter: "all",
      sexFilter: "all",
      ageFilter: "all",
      diseaseFilter: "all",
      /** these columns are all the critearia that the rows should be split on. */
      splitByColumn: ["sample_type", "sex", "age_range", "disease"],
      splittableColumns: [
        "sample_type",
        "sex",
        "age_range",
        "disease",
        "number_of_samples",
      ],
      factsProperties: [
        "sample_type.label",
        "sex.label",
        "age_range.label",
        "disease.label",
        "disease.name",
        "number_of_samples",
        "number_of_donors",
      ],
    };
  },
  computed: {
    materialtypeOptions() {
      return [
        ...new Set(
          this.facts
            .map((fact) => fact.sample_type)
            .sort((a, b) => a.localeCompare(b))
        ),
      ];
    },
    sexOptions() {
      return [
        ...new Set(
          this.facts.map((fact) => fact.sex).sort((a, b) => a.localeCompare(b))
        ),
      ];
    },
    ageRangeOptions() {
      return [
        ...new Set(
          this.facts
            .map((fact) => fact.age_range)
            .sort((a, b) => a.localeCompare(b))
        ),
      ];
    },
    diseaseOptions() {
      return [
        ...new Set(
          this.facts
            .map((fact) => fact.disease)
            .sort((a, b) => a.localeCompare(b))
        ),
      ];
    },
    columnChecked() {
      return (column) => this.splitByColumn.includes(column);
    },
    factsTable() {
      if (this.filters.length === 0) return this.facts;
      const filteredFacts = [];

      const lastFilterIndex = this.filters.length - 1;

      for (const fact of this.facts) {
        for (const [index, filter] of this.filters.entries()) {
          const propertyValue = this.getValue(fact, filter.column);
          /** it did not match all filters, so goodbye. */
          if (!propertyValue && filter.value !== "Unknown") {
            break;
          } else if (!propertyValue && filter.value === "Unknown") {
            filteredFacts.push(fact);
          } else if (propertyValue !== filter.value) {
            break;
          } else if (index === lastFilterIndex) {
            filteredFacts.push(fact);
          }
        }
      }

      return filteredFacts;
    },
  },
  methods: {
    toggleColumn(e, columnName) {
      if (e.target.checked) {
        this.splitByColumn.push(columnName);
      } else {
        const newArray = this.splitByColumn.filter((sbc) => sbc !== columnName);
        this.splitByColumn = newArray;
      }
      this.sampleFilter = "all";
      this.sexFilter = "all";
      this.ageFilter = "all";
      this.diseaseFilter = "all";
      this.filters = [];
      this.collapseRows();
    },
    hasAFactToShow(fact) {
      const hasSamples =
        fact.number_of_samples && parseInt(fact.number_of_samples) !== 0;
      const hasDonors =
        fact.number_of_donors && parseInt(fact.number_of_samples) !== 0;
      const facts = [hasSamples, hasDonors, fact.sex];

      /** return true, if any of the facts is filled it. */
      return facts.some((fact) => fact);
    },
    filter(column, event) {
      const indexToRemove = this.filters.findIndex(
        (fa) => fa.column === column
      );

      if (indexToRemove > -1) {
        this.filters.splice(indexToRemove, 1);
      }

      if (event.target.value !== "all") {
        this.filters.push({ column, value: event.target.value });
      }
    },
    sort(column) {
      /** user clicked again */
      if (this.sortColumn === column) {
        this.sortAsc = !this.sortAsc;
      } else {
        this.sortColumn = column;
        this.sortAsc = true;
      }

      let newFacts = this.hardcopy(this.facts);

      newFacts.sort((factA, factB) => {
        const factAProperty = this.getValue(factA, column);
        const factBProperty = this.getValue(factB, column);

        const factValueA = isNaN(factAProperty)
          ? factAProperty
          : parseInt(factAProperty);

        const factValueB = isNaN(factBProperty)
          ? factBProperty
          : parseInt(factBProperty);

        if (factValueA > factValueB) {
          return this.sortAsc ? 1 : -1;
        } else if (factValueA < factValueB) {
          return this.sortAsc ? -1 : 1;
        }

        return 0;
      });
      this.facts = newFacts;
      /** if we do not key this, then it will break */
      this.tableVersion = this.tableVersion + 1;
    },
    getValue(object, propertyString) {
      const trail = propertyString.split(".");
      const trailLength = trail.length;

      let value;
      let next = object;
      for (let trailIndex = 0; trailIndex < trailLength; trailIndex++) {
        const trailPart = trail[trailIndex];

        if (!next[trailPart]) return value ?? "Unknown";
        else {
          value = next[trailPart];
          next = next[trailPart];
        }
      }
      return value ?? "Unknown";
    },
    hardcopy(value) {
      return JSON.parse(JSON.stringify(value));
    },
    collapseRows() {
      if (this.splitByColumn.length === 4) {
        /** no group together selected, so reset the state */
        this.copyFactsToComponentState();
        return;
      }

      /** make a copy that we can keep mutating utill we have dealt with all the collapses.
       * order matters!
       */
      const baseFacts = this.hardcopy(this.factsData());
      const groupedFacts = [];

      const criteriaMet = [];

      for (const baseFact of baseFacts) {
        const criteria = {};

        let newCriteria = "";

        for (const criteriaColumn of this.splittableColumns) {
          if (this.splitByColumn.includes(criteriaColumn)) {
            const critValue = this.getValue(baseFact, criteriaColumn);
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
              (obj) => obj[critKey] === criteria[critKey]
            );
          }
          criteriaMet.push(newCriteria);
          groupedFacts.push(critGroup);
        }
      }
      const collapsedFacts = [];

      for (const factGroup of groupedFacts) {
        let collapsedFact = {};

        for (const fact of factGroup) {
          if (!Object.keys(collapsedFact).length) {
            collapsedFact = fact;
            continue;
          }

          for (const column of this.splittableColumns) {
            if (!this.splitByColumn.includes(column)) {
              const mergedValue = collapsedFact[column];

              if (Array.isArray(mergedValue)) {
                const valueToMerge = fact[column];
                if (Array.isArray(valueToMerge)) {
                  collapsedFact[column] = [
                    ...new Set(...collapsedFact[column].concat(fact[column])),
                  ];
                } else {
                  if (!collapsedFact[column].includes(fact[column])) {
                    collapsedFact[column].push(fact[column]);
                  }
                }
              } else {
                if (collapsedFact[column] !== fact[column]) {
                  if (isNaN(collapsedFact[column]) && isNaN(fact[column])) {
                    collapsedFact[column] = [
                      collapsedFact[column],
                      fact[column],
                    ];
                  } else {
                    collapsedFact[column] =
                      collapsedFact[column] + fact[column];
                  }
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

      this.facts = collapsedFacts;
    },
    factsData() {
      const rawFacts = this.hardcopy(this.attribute);

      const facts = [];

      for (const rawFact of rawFacts) {
        const fact = {};
        for (const property of this.factsProperties) {
          const key = property.split(".")[0];

          if (!fact[key]) {
            fact[key] = this.getValue(rawFact, property);
          }
        }
        facts.push(fact);
      }
      return facts;
    },
    copyFactsToComponentState() {
      this.facts = this.factsData();
    },
  },
  mounted() {
    this.copyFactsToComponentState();
  },
};
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
}

.filter-bar th {
  border-top: none;
  white-space: nowrap;
}
</style>
