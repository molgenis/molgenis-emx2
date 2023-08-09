<template>
  <div class="mt-4" v-if="attribute.value && attribute.value.length">
    <table class="mb-4">
      <tbody>
        <tr>
          <th>Number of samples:</th>
          <td class="pl-4">{{ numberOfSamples }}</td>
        </tr>
        <tr>
          <th>Number of donors:</th>
          <td class="pl-4">{{ numberOfDonors }}</td>
        </tr>
      </tbody>
    </table>
    <table class="table border w-100">
      <thead>
        <tr class="facts-header bg-secondary text-white">
          <th @click="sort('sample_type.label')">Material type</th>
          <th @click="sort('number_of_samples')">Samples</th>
          <th @click="sort('sex.CollectionSex')">Sex</th>
          <th @click="sort('number_of_donors')">Donors</th>
          <th @click="sort('age.CollectionAgeRange')">Age range</th>
          <th @click="sort('disease.id')">Disease codes</th>
        </tr>
        <tr class="filter-bar">
          <th>
            <select @change="filter('sample_type.label', $event)" class="w-100">
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
          <th></th>
          <th>
            <select
              @change="filter('sex.CollectionSex', $event)"
              class="text-right"
            >
              <option value="all">All</option>
              <option v-for="sex of sexOptions" :key="sex" :value="sex">
                {{ sex }}
              </option>
            </select>
          </th>
          <th></th>
          <th>
            <select
              @change="filter('age.CollectionAgeRange', $event)"
              class="text-right"
            >
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
          <th></th>
        </tr>
      </thead>
      <tbody>
        <template v-for="fact of factsTable">
          <tr :key="fact.id" v-if="hasAFactToShow(fact)">
            <th scope="row" class="pr-1 align-top text-nowrap">
              {{ fact.sample_type.label }}
            </th>
            <td>{{ fact.number_of_samples || "-" }}</td>
            <td>{{ fact.sex ? fact.sex.CollectionSex : "-" }}</td>
            <td>{{ fact.number_of_donors || "-" }}</td>
            <td>{{ fact.age ? fact.age.CollectionAgeRange : "-" }}</td>
            <td v-if="fact.disease && fact.disease.length">
              <div
                v-for="disease in fact.disease"
                :key="disease.id"
                class="badge"
              >
                {{ disease.id }}
              </div>
            </td>
            <td v-else>-</td>
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
      type: Object,
    },
  },
  data() {
    return {
      facts: [],
      sortColumn: "",
      sortAsc: false,
      filters: [],
    };
  },
  computed: {
    materialtypeOptions() {
      return [
        ...new Set(this.attribute.value.map((attr) => attr.sample_type.label)),
      ];
    },
    numberOfSamples() {
      if (this.factsTable.length === 0) return 0;

      return this.factsTable
        .map((attr) => parseInt(attr.number_of_samples))
        .reduce((a, b) => a + b);
    },
    numberOfDonors() {
      if (this.factsTable.length === 0) return 0;

      return this.factsTable
        .map((attr) => parseInt(attr.number_of_donors))
        .reduce((a, b) => a + b);
    },
    sexOptions() {
      return [
        ...new Set(this.attribute.value.map((attr) => attr.sex.CollectionSex)),
      ];
    },
    ageRangeOptions() {
      return [
        ...new Set(
          this.attribute.value.map((attr) => attr.age.CollectionAgeRange)
        ),
      ];
    },
    factsTable() {
      if (this.filters.length === 0) return this.facts;
      const filteredFacts = [];

      const lastFilterIndex = this.filters.length - 1;

      for (const fact of this.facts) {
        for (const [index, filter] of this.filters.entries()) {
          const propertyValue = this.getPropertyValue(fact, filter.column);
          /** it did not match all filters, so goodbye. */
          if (propertyValue !== filter.value) {
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

      this.facts.sort((factA, factB) => {
        const factAProperty = this.getPropertyValue(factA, column);
        const factBProperty = this.getPropertyValue(factB, column);

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
    },
    getPropertyValue(object, propertyString) {
      const trail = propertyString.split(".");
      const trailLength = trail.length;

      // could be recursive, but out of scope for now
      switch (trailLength) {
        case 1: {
          return object[trail[0]];
        }
        case 2: {
          return object[trail[0]][trail[1]];
        }
        case 3: {
          return object[trail[0]][trail[1]][trail[2]];
        }
      }
    },
  },
  mounted() {
    this.facts = Object.assign([], this.attribute.value);
  },
};
</script>

<style scoped>
tr th:not(:first-child),
tr td:not(:first-child) {
  text-align: right;
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
