<template>
  <div v-if="cohort" class="container">
    <grid-block>
      <page-header
        :title="cohort.name"
        :subTitle="cohort.institution ? cohort.institution[0].name : null"
        :logoUrl="cohort.logo.url"
        :subTitleLink="
          cohort.institution
            ? { to: '/institutions/' + cohort.institution[0].pid }
            : null
        "
      ></page-header>
    </grid-block>

    <grid-block>
      <links-list :isHorizontal="true" :items="mainLinks"></links-list>
    </grid-block>

    <div class="card-columns card-columns-2">
      <key-value-block
        :items="[{ label: 'Description', value: cohort.description }]"
      ></key-value-block>

      <key-value-block
        v-if="cohort.designPaper"
        :items="[{ label: 'Marker paper', value: cohort.designPaper.title }]"
      ></key-value-block>

      <key-value-block
        heading="General design"
        :items="generalDesignItems"
      ></key-value-block>
    </div>

    <grid-block
      heading="Contributors"
      v-if="cohort.contributors && cohort.contributors.length"
    >
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

    <grid-block heading="Partners" v-if="partners.length">
      <div class="card-columns">
        <image-card
          v-for="(partner, index) in partners"
          :key="index"
          :title="partner.institution.name"
          :linkUrl="`/institutions/${partner.institution.pid}`"
        >
          <template #image>
            <image-display
              :url="partner.institution.logo.url"
              :alt="partner.institution.pid"
            ></image-display>
          </template>
        </image-card>
      </div>
    </grid-block>

    <grid-block heading="Networks" v-if="networks.length">
      <div class="card-columns">
        <image-card v-for="(network, index) in networks" :key="index">
          <template #image>
            <image-display
              :url="network.logo.url"
              :alt="network.name"
            ></image-display>
          </template>
          <template #body>
            <h5 class="card-title">{{ network.name }}</h5>
            <p class="card-text">{{ network.description }}</p>
            <p class="card-text">
              <small class="text-muted">
                <router-link
                  :to="`/networks/${network.pid}`"
                  class="stretched-link"
                  >LEARN MORE
                </router-link>
              </small>
            </p>
          </template>
        </image-card>
      </div>
    </grid-block>

    <grid-block heading="Available data & samples">
      <strong>Data categories</strong>
      <p>{{ dataCategories.join(", ") }}</p>
      <strong>Sample categories</strong>
      <p>{{ sampleCategories.join(", ") }}</p>
      <strong>Areas of information</strong>
      <p>{{ areasOfInformation.join(", ") }}</p>
    </grid-block>

    <grid-block heading="Documentation" v-if="documentation.length">
      <links-list :items="documentation"></links-list>
    </grid-block>

    <grid-block heading="Subpopulations" v-if="subpopulations.length">
      <table-display
        :isClickable="true"
        @row-click="$router.push({ path: $event._path })"
        :columns="[
          { name: 'name', label: 'Name' },
          { name: 'description', label: 'Description' },
          { name: 'numberOfParticipants', label: 'Number of participants' },
          { name: 'ageGroups', label: 'Age categories' },
        ]"
        :rows="subpopulations"
      ></table-display>
    </grid-block>

    <grid-block heading="Collection events" v-if="collectionEvents.length">
      <table-display
        :isClickable="true"
        @row-click="$router.push({ path: $event._path })"
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
</style>

<script>
import { fetchById } from "../../store/repository/cohortRepository";
import { startEndYear } from "../../filters";
import {
  PageHeader,
  GridBlock,
  KeyValueBlock,
  ImageDisplay,
  ContactDisplay,
  LinksList,
  TableDisplay,
  ImageCard,
} from "@mswertz/emx2-styleguide";

const networkNoLogoUrl =
  "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAKoAAABkCAYAAAAWlKtGAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAJbSURBVHgB7dzNbdRgEAbgl58DR0r4SuDIkRLoINsBdJDtADoIVAIdhA5SAmdOwVaUS5SNvGt712M/jzRXKxq/mtjJ50kAAAAAAAAAAAAAAAAAAACYyfuurru66+r+xLrt6qarlvXQlwW56upvTr8RT6u/1tfUpy8L8iXT3YindZW69GVBWqadGM9NkPepp2WjfXmTZfrW1cfM511X/7r6nVr0ZWH6h/z7mesu9ejLwhxqYsvx2gvXq2azfXmVZTrUrFN/3qmvdymb7cvrQAGCSglvs20Vn1M3yUSlBEGlBEGlBEGlBEGlhK2/9a/lD/6rZ6JSgqBSQrWgthyvZf1ajtfCaP1Rs7mPs92mns32ZakT9Wfm9yEPB5ErnfQ/R1/+hMH68Mz5ycXTg8KfU8Pcfemv3cJRdjlPUB/rJjVu0i7z9cCXqCfqG3euyfo4XXdZvqn70l9rF0ZpXf3IuO+F+gB+z/CXkZssf7q2TNOXfWp+kbtqLQ83d03TlRXbZV3TlRXrf931jwNDp6sXDS7qU4ZP118xXbmwfYa/hFwHLqhl+HS9i+nKhe1julJEi+lKIfuYrhTR8vDGP3S6Dp3Ez5V15Yx2zrMH1pUzSsvwf8NOUVeBEXY5z4n7qmvcWZCW84R1HxjJunJKOBSuluO1F663adU2hSzRoRBtfY37pCygoARBpQRBpQRBpQRBpQRBpQRBpQRBpQRBpQRBpQRBpQRBnU/L8VpgJv0RvLmP+VVc4z4pE3U868opwbpyythlvqD6EpVJWVdOGS3WlQMAAAAAAAAAAADA4vwH56V4ljC8O5IAAAAASUVORK5CYII=";

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
    ImageCard,
  },
  data() {
    return {
      cohort: null,
      metaData: null,
    };
  },
  computed: {
    mainLinks() {
      const items = [];
      if (this.cohort.website) {
        items.push({
          label: "Website",
          href: this.cohort.website,
        });
      }
      if (this.cohort.contactEmail) {
        items.push({
          label: "Contact",
          href: this.cohort.contactEmail,
        });
      }
      return items;
    },
    generalDesignItems() {
      return [
        {
          label: "Cohort type",
          value: this.cohort.type
            ? this.cohort.type.map((type) => type.name).join(", ")
            : "not available",
        },
        {
          label: "Design",
          value: this.cohort.design ? this.cohort.design.name : "not available",
        },
        {
          label: "Collection type",
          value: this.cohort.collectionType
            ? this.cohort.collectionType[0].name
            : "not available",
        },
        {
          label: "Start/End year",
          value: startEndYear(this.cohort.startYear, this.cohort.endYear),
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
          value: this.cohort.populationAgeGroups
            ? this.cohort.populationAgeGroups.map((pag) => pag.name)
            : [],
        },
      ];
    },
    partners() {
      if (!this.cohort.partners) {
        return [];
      } else {
        // only show partners that have a log set
        return this.cohort.partners.filter((partner) => {
          return (
            partner &&
            partner.institution &&
            partner.institution.logo &&
            partner.institution.logo.url
          );
        });
      }
    },
    networks() {
      if (!this.cohort.networks) {
        return [];
      } else {
        return this.cohort.networks
          .map((network) => {
            if (network.description && network.description.length > 200) {
              network.description =
                network.description.substring(0, 200) + " ...";
            }
            return network;
          })
          .map((network) => {
            if (!network.logo || !network.logo.url) {
              network.logo = {
                url: networkNoLogoUrl,
              };
            }
            return network;
          });
      }
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
              description: subcohort.description,
              numberOfParticipants: subcohort.numberOfParticipants,
              ageGroups: !subcohort.ageGroups
                ? ""
                : subcohort.ageGroups
                    .map(topLevelAgeGroup)
                    .reduce((ageGroups, ageGroup) => {
                      if (!ageGroups.find((ag) => ageGroup.name === ag.name)) {
                        ageGroups.push(ageGroup);
                      }
                      return ageGroups;
                    }, [])
                    .map((ag) => ag.name)
                    .join(","),
              _path: `${this.$route.path}/subcohorts/${subcohort.name}`,
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
                const startYear =
                  item.startYear && item.startYear.name
                    ? item.startYear.name
                    : null;
                const endYear =
                  item.endYear && item.endYear.name ? item.endYear.name : null;
                return startEndYear(startYear, endYear);
              })(),
              _path: `${this.$route.path}/collection-events/${item.name}`,
            };
          });
    },
    dataCategories() {
      if (!this.cohort.collectionEvents) {
        return [];
      } else {
        return this.eventDetailSummary("dataCategories");
      }
    },
    sampleCategories() {
      if (!this.cohort.collectionEvents) {
        return [];
      } else {
        return this.eventDetailSummary("sampleCategories");
      }
    },
    areasOfInformation() {
      if (!this.cohort.collectionEvents) {
        return [];
      } else {
        return this.eventDetailSummary("areasOfInformation");
      }
    },
    documentation() {
      if (!this.cohort.documentation) {
        return [];
      }
      return this.cohort.documentation.map((d) => {
        return {
          text: d.name,
          href: d.url,
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
    eventDetailSummary(detail) {
      return Array.from(
        new Set(
          this.cohort.collectionEvents
            .filter((collectionEvent) => collectionEvent[detail])
            .flatMap((collectionEvent) => {
              return collectionEvent[detail];
            })
            .map((detail) => detail.name)
        )
      );
    },
  },
  mounted: async function () {
    this.fetchData();
  },
};
</script>
