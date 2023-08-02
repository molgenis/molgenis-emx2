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
            <select @change="filter('sample_type', $event)" class="w-100">
              <option value="all">All</option>
              <option
                v-for="material of materialtypeOptions"
                :key="material"
                :value="renderValue(material)"
              >
                {{ renderValue(material) }}
              </option>
              <option value="Unknown">Unknown</option>
            </select>
          </th>
          <th>
            <select @change="filter('sex', $event)">
              <option value="all">All</option>
              <option
                v-for="sex of sexOptions"
                :key="sex"
                :value="renderValue(sex)"
              >
                {{ renderValue(sex) }}
              </option>
              <option value="Unknown">Unknown</option>
            </select>
          </th>

          <th>
            <select @change="filter('age_range', $event)">
              <option value="all">All</option>
              <option
                v-for="ageRange of ageRangeOptions"
                :key="ageRange"
                :value="renderValue(ageRange)"
              >
                {{ renderValue(ageRange) }}
              </option>
              <option value="Unknown">Unknown</option>
            </select>
          </th>
          <th>
            <select @change="filter('disease', $event)">
              <option value="all">All</option>
              <option
                v-for="disease of diseaseOptions"
                :key="disease"
                :value="renderValue(disease)"
              >
                {{ renderValue(disease) }}
              </option>
              <option value="Unknown">Unknown</option>
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
              {{ renderValue(fact.sample_type, "label") }}
            </th>
            <td>{{ renderValue(fact.sex) }}</td>
            <td>{{ renderValue(fact.age_range) }}</td>
            <td :title="renderValue(fact.disease_name)">
              {{ renderValue(fact.disease) }}
            </td>
            <td>{{ renderValue(fact.number_of_donors) }}</td>
            <td>{{ renderSamplesValue(fact.number_of_samples) }}</td>
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
          this.attribute.map((attr) => attr.sample_type?.label).filter((l) => l)
        ),
      ];
    },
    sexOptions() {
      return [
        ...new Set(
          this.attribute.map((attr) => attr.sex?.label).filter((l) => l)
        ),
      ];
    },
    ageRangeOptions() {
      return [
        ...new Set(
          this.attribute.map((attr) => attr.age_range?.label).filter((l) => l)
        ),
      ];
    },
    diseaseOptions() {
      return [
        ...new Set(
          this.attribute.map((attr) => attr.disease?.label).filter((l) => l)
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
    renderValue(value) {
      if (Array.isArray(value)) {
        return value.join(", ");
      } else return value;
    },
    renderSamplesValue(value) {
      if (!value) return "Unknown";

      if (Array.isArray(value)) {
        const sum = value.reduce(
          (prev, next) => parseInt(prev) + parseInt(next)
        );
        return sum;
      } else return value;
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
                  collapsedFact[column] = [collapsedFact[column], fact[column]];
                }
              }
            }
          }
        }
        collapsedFact.number_of_donors = "Available";
        collapsedFacts.push(collapsedFact);
      }
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
}
</style>
