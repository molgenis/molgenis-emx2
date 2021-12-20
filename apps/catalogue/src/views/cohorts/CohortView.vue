<template>
  <div v-if="cohort" class="container">
    <grid-block>
      <page-header
        :title="cohort.name"
        :logoUrl="cohort.logo.url"
      ></page-header>
    </grid-block>

    <grid-block>
      <links-list
        :items="[
          {
            label: 'Website',
            href: cohort.website,
          },
          {
            label: 'Contact',
            href: cohort.contactEmail,
          },
        ]"
      ></links-list>
    </grid-block>

    <div class="card-columns card-columns-2">
      <key-value-block
        :items="[{ label: 'Description', value: cohort.description }]"
      ></key-value-block>

      <key-value-block
        :items="[{ label: 'Marker paper', value: cohort.designPaper }]"
      ></key-value-block>

      <key-value-block
        heading="General design"
        :items="generalDesignItems"
      ></key-value-block>
    </div>

    <grid-block heading="Contributors">
      <div class="card-columns">
        <contact-display
          v-for="(contributor, index) in cohort.contributors"
          :key="index"
          :contact="contributor.contact"
          :contributionType="contributor.contributionType"
          :contributionDescription="contributor.contributionDescription"
        ></contact-display>
      </div>
    </grid-block>

    <grid-block heading="Partners">
      <div v-for="(partner, index) in cohort.partners" :key="index">
        <image-display
          v-if="
            partner &&
            partner.institution &&
            partner.institution.logo &&
            partner.institution.logo.url
          "
          style="width: 100px; height: 100px"
          :url="partner.institution.logo.url"
        ></image-display>
      </div>
    </grid-block>

    <grid-block heading="Networks">
      <div class="card-deck">
        <div
          class="card"
          v-for="(network, index) in cohort.networks"
          :key="index"
        >
          <image-display
            v-if="network && network.logo && network.logo.url"
            :url="network.logo.url"
            :alt="network.name"
          ></image-display>
          <div v-else>{{ network.pid }}</div>
        </div>
      </div>
    </grid-block>

    <grid-block heading="Available data & samples">
      <strong>Data categories</strong>
      <strong>Areas of information</strong>
      <strong>Sample categories</strong>
    </grid-block>

    <grid-block heading="Subpopulations">
      <table-display
        :columns="[
          { name: 'name', label: 'Name' },
          { name: 'description', label: 'Description' },
          { name: 'numberOfParticipants', label: 'Number of participants' },
          { name: 'ageGroups', label: 'Age categories' },
        ]"
        :rows="subpopulations"
      ></table-display>
    </grid-block>

    <grid-block heading="Collection events">
      <table-display
        :columns="[
          { name: 'name', label: 'Name' },
          { name: 'description', label: 'Description' },
          { name: 'startAndEndYear', label: 'Start and end year' },
        ]"
        :rows="collectionEvents"
      ></table-display>
    </grid-block>

    <div class="card-columns card-columns-2">
      <grid-block heading="Access conditions">
        <ul>
          <li
            v-for="(condition, index) in cohort.dataAccessConditions"
            :key="index"
          >
            {{ condition.name }}
          </li>
        </ul>
        <p>{{ cohort.dataAccessConditionsDescription }}</p>
        <p>{{ cohort.releaseDescription }}</p>
      </grid-block>

      <grid-block heading="Linkage options">
        <p>
          {{ cohort.linkageOptions }}
        </p>
      </grid-block>
    </div>
  </div>
</template>

<style scoped>
@media (min-width: 576px) {
  .card-columns-2 {
    column-count: 2;
  }
}

.card {
  border: 0;
}
</style>

<script>
import { fetchById } from "../../store/repository/cohortRepository";
// import { fetchSchemaMetaData } from "store/repository/metaDataRepository";
import {
  PageHeader,
  GridBlock,
  KeyValueBlock,
  ImageDisplay,
  ContactDisplay,
  LinksList,
  TableDisplay,
} from "@mswertz/emx2-styleguide";

export default {
  name: "CohortView",
  components: {
    GridBlock,
    PageHeader,
    LinksList,
    KeyValueBlock,
    ImageDisplay,
    ContactDisplay,
    TableDisplay,
  },
  data() {
    return {
      cohort: null,
      metaData: null,
    };
  },
  computed: {
    generalDesignItems() {
      return [
        {
          label: "Type",
          value: this.cohort.collectionType
            ? this.cohort.collectionType[0].name
            : "na",
        },
        {
          label: "Design",
          value: this.cohort.design ? this.cohort.design.name : "na",
        },
        {
          label: "Collection type",
          value: this.cohort.collectionType
            ? this.cohort.collectionType[0].name
            : "",
        },
        {
          label: "Start/End year",
          value:
            (this.cohort.startYear || "N/A") +
            " - " +
            (this.cohort.endYear || "N/A"),
        },
        {
          label: "Population",
          value: this.cohort.countries
            ? [...this.cohort.countries]
                .sort((a, b) => a.order - b.order)
                .map((c) => c.name)
                .join(",")
            : "",
        },
        {
          label: "Number of participants",
          value: this.cohort.numberOfParticipants,
        },
        {
          label: "Age group at inclusion",
          value: this.cohort.populationAgeGroups.map((pag) => pag.name),
        },
      ];
    },
    subpopulations() {
      const topLevelAgeGroup = (ageGroup) => {
        if (!ageGroup.parent) {
          return ageGroup;
        }
        return topLevelAgeGroup(ageGroup.parent);
      };
      return !this.cohort.subcohorts
        ? []
        : this.cohort.subcohorts.map((subcohort) => {
            return {
              name: subcohort.name,
              desciption: subcohort.desciption,
              numberOfParticipants: subcohort.numberOfParticipants,
              ageGroups: subcohort.ageGroups
                .map(topLevelAgeGroup)
                .reduce((ageGroups, ageGroup) => {
                  if (!ageGroups.find((ag) => ageGroup.name === ag.name)) {
                    ageGroups.push(ageGroup);
                  }
                  return ageGroups;
                }, [])
                .map((ag) => ag.name)
                .join(","),
            };
          });
    },
    collectionEvents() {
      return !this.cohort.collectionEvents
        ? []
        : this.cohort.collectionEvents.map((item) => {
            return {
              name: item.name,
              description: item.description,
              startAndEndYear: (() => {
                let value =
                  ((item.startYear && item.startYear.name) || "n/a") +
                  " - " +
                  ((item.endYear && item.endYear.name) || "n/a");
                return value === "n/a - n/a" ? null : value;
              })(),
            };
          });
    },
    cohortMetaData() {
      return this.metaData._schema.tables.find(
        (table) => table.name == "Cohorts"
      );
    },
  },
  methods: {
    async fetchData() {
      this.cohort = await fetchById(this.$route.params.pid);
    },
    // async fetchMetaData() {
    //   this.metaData = await fetchSchemaMetaData();
    // },
  },
  mounted: async function () {
    this.fetchData();
    // this.fetchMetaData();
  },
};
</script>
