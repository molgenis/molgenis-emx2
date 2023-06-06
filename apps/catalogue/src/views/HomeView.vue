<template>
  <div class="">
    <div class="row">
      <div class="col">
        <BannerImage
          imageUrl="https://image.focuspoints.io/general-1.jpg?_jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJmb2N1c1BvaW50WSI6MC4wLCJmb2N1c1BvaW50WCI6MC4wLCJ3aWR0aCI6MTQ0MCwiaXNzIjoidW1jZyIsImFjdGlvbiI6InRyYW5zZm9ybSIsInVybCI6Imh0dHBzOi8vdW1jZ3Jlc2VhcmNoLm9yZy9kb2N1bWVudHMvNzcwNTM0Lzc3NTkwNy9nZW5lcmFsLTEuanBnL2NlNGVkMTM5LTMxYzMtOTg3Mi02NjBiLWU3ZjVjNDFhNTY0OD90PTE2Mjk3MDYzODQyOTMmZG93bmxvYWQ9dHJ1ZSIsImhlaWdodCI6MzYwfQ.ulaZWsVt6k6Uil4zLdaxpnLrWZJubDttUIlE5hr5yqgXW7ACAD5nF1Kpl4R-Wd2QU2haLYJt0zvzMWv2843gfA"
          title="Cohorts, biobanks and dataset of the UMCG"
          subTitle="Universitair Medisch Centrum Groningen, the Netherlands" />
      </div>
    </div>

    <div class="row justify-content-md-center py-3">
      <div class="col-sm-12 col-md-8">
        <search-resource
          :resourceType="resourceType"
          placeholder="Search the UMCG cohorts" />
      </div>
    </div>

    <div class="row">
      <div class="col">
        <div class="card-deck">
          <IconCard cardTitle="Datasets" icon="DatabaseIcon" footerText="">
            <div class="card-text">
              <h1 class="text-center">{{ cohortCount + bioBankCount }}</h1>
              <ul class="card-text">
                <li>Cohorts: {{ cohortCount }}</li>
                <li>Biobanks: {{ bioBankCount }}</li>
              </ul>
            </div>
          </IconCard>

          <IconCard cardTitle="Participants" icon="UsersIcon" footerText="">
            <div class="card-text">
              <h1 class="text-center">
                {{
                  Math.round(participantCount / 10000 + Number.EPSILON) / 100
                }}M
              </h1>
              <p class="text-center">
                in total > 1,000: {{ participantPercentageAboveOneThousand }}%
              </p>
            </div>
          </IconCard>

          <IconCard cardTitle="Samples" icon="TestPipeIcon" footerText="">
            <div class="card-text">
              <h1 class="text-center">
                {{
                  Math.round(
                    (biologicalSamplesCounts / dataCategoriesCounts) * 100
                  )
                }}%
              </h1>
              <ul>
                <li>
                  Fluid:
                  {{
                    Math.round(
                      (sampleTypeCounts["Fluid"] / sampleTypesTotalCount) * 100
                    )
                  }}%
                </li>
                <li>
                  Genetic:
                  {{
                    Math.round(
                      (sampleTypeCounts["Genetic material"] /
                        sampleTypesTotalCount) *
                        100
                    )
                  }}%
                </li>
                <li>
                  Tissue:
                  {{
                    Math.round(
                      (sampleTypeCounts["Tissue"] / sampleTypesTotalCount) * 100
                    )
                  }}%
                </li>
              </ul>
            </div>
          </IconCard>

          <IconCard cardTitle="Cohort design" icon="ToolsIcon" footerText="">
            <div class="card-text">
              <h1 class="text-center" style="color: white">spacer</h1>
              <ul>
                <li v-for="type in Object.keys(designTypeCounts)" :key="type">
                  {{ type }}:
                  {{
                    Math.round(
                      (designTypeCounts[type] / designTypesTotalCount) * 100
                    )
                  }}%
                </li>
              </ul>
            </div>
          </IconCard>

          <IconCard cardTitle="Longitudinal" icon="ClockIcon" footerText="">
            <div class="card-text">
              <h1 class="text-center">{{ percentageLongitudinalStudies }}%</h1>
            </div>
          </IconCard>
        </div>
      </div>
    </div>

    <div class="row mt-3">
      <div class="col">
        <div class="card-deck">
          <IconCard cardTitle="Recently added" icon="CirclePlusIcon">
            <div class="card-text">
              <ul class="card-text">
                <li v-for="(added, index) in recentlyAdded" :key="index">
                  {{ added.date }} - <span v-html="added.html"></span>
                </li>
                <li v-if="recentlyAdded.length === 0">
                  No recently added items
                </li>
              </ul>
            </div>
          </IconCard>

          <IconCard cardTitle="News" icon="NewsIcon">
            <div class="card-text">
              <ul class="card-text">
                <li v-for="(newsItem, index) in newsItems" :key="index">
                  {{ newsItem.date }} - <span v-html="newsItem.html"></span>
                </li>
                <li v-if="newsItems.length === 0">No news items</li>
              </ul>
            </div>
          </IconCard>
        </div>
      </div>
    </div>

    <div class="row mt-3">
      <div class="col">
        <div class="card-deck">
          <IconCard
            cardTitle="Cohort & Biobank Coordination hub"
            icon="AffiliateIcon">
            <div class="card-text">
              <address>
                <strong>University Medical Center Groningen (UMCG)</strong
                ><br />
                Antonius Deusinglaan 1<br />
                9713 AV Groningen<br />
                The Netherlands<br />
                <abbr title="Phone"></abbr> (123) 456-7890
              </address>
            </div>
          </IconCard>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import SearchResource from "../components/SearchResource.vue";
import BannerImage from "../components/display/BannerImage.vue";
import IconCard from "../components/display/IconCard.vue";
import homeViewQuery from "../store/query/homeView.js";

import {} from "vue-tabler-icons";

export default {
  name: "HomeView",
  components: {
    SearchResource,
    BannerImage,
    IconCard,
  },
  data() {
    return {
      iconSize: "62",
      cohortCount: 0,
      bioBankCount: 0,
      participantCount: 0,
      participantPercentageAboveOneThousand: 0,
      percentageLongitudinalStudies: 0,
      designTypeCounts: {},
      dataCategoriesCounts: 0,
      biologicalSamplesCounts: 0,
      sampleTypeCounts: {
        Fluid: 0,
        "Genetic material": 4,
        Tissue: 0,
      },
      newsItems: [],
      recentlyAdded: [],
      graphqlError: "",
    };
  },
  props: {
    resourceType: {
      type: String, // one of Resource
      default: () => "Cohorts",
    },
  },
  computed: {
    designTypesTotalCount() {
      return Object.values(this.designTypeCounts).reduce(
        (total, count) => total + count,
        0
      );
    },
    sampleTypesTotalCount() {
      return Object.values(this.sampleTypeCounts).reduce(
        (total, count) => total + count,
        0
      );
    },
  },
  methods: {
    async load() {
      const resp = await request("graphql", homeViewQuery).catch(error => {
        console.log(error);
      });

      this.cohortCount = resp.Cohorts_agg.count;
      this.bioBankCount = resp.Databanks_agg.count;

      this.participantCount = resp.Cohorts.filter(c => c.numberOfParticipants)
        .map(c => c.numberOfParticipants)
        .reduce((a, b) => a + b, 0);

      this.participantPercentageAboveOneThousand = Math.round(
        (resp.Cohorts.filter(
          c => c.numberOfParticipants && c.numberOfParticipants > 1000
        ).length /
          resp.Cohorts.length) *
          100
      );

      this.percentageLongitudinalStudies = Math.round(
        (resp.Cohorts.filter(c => c.design && c.design.name === "Longitudinal")
          .length /
          resp.Cohorts.length) *
          100
      );

      this.designTypeCounts = resp.Cohorts.filter(c => c.collectionType).reduce(
        (count, c) => {
          c.collectionType.forEach(collectionType => {
            const type = collectionType.name;
            if (count[type] >= 0) {
              count[type] = count[type] + 1;
            } else {
              count[type] = 0;
            }
          });

          return count;
        },
        {}
      );

      // total number of selected dataCategories
      this.dataCategoriesCounts = resp.Cohorts.reduce((total, cohort) => {
        return (total +=
          total + cohort.collectionEvents
            ? cohort.collectionEvents.reduce((perCohort, collectionEvent) => {
                return (perCohort += collectionEvent.dataCategories
                  ? collectionEvent.dataCategories.length
                  : 0);
              }, total)
            : 0);
      }, 0);

      // total number of Biological samples dataCategories
      this.biologicalSamplesCounts = resp.Cohorts.reduce((total, cohort) => {
        return (total +=
          total + cohort.collectionEvents
            ? cohort.collectionEvents.reduce((perCohort, collectionEvent) => {
                return (perCohort += collectionEvent.dataCategories
                  ? collectionEvent.dataCategories
                      .map(dc => dc.name)
                      .includes("Biological samples")
                    ? 1
                    : 0
                  : 0);
              }, total)
            : 0);
      }, 0);

      // sample types
      this.sampleTypeCounts = resp.Cohorts.reduce((sampleTypes, cohort) => {
        if (cohort.collectionEvents) {
          sampleTypes = cohort.collectionEvents.reduce(
            (sampleTypes, collectionEvent) => {
              if (collectionEvent.sampleCategories) {
                if (
                  collectionEvent.sampleCategories.name ===
                    "Fluids and Secretions" ||
                  collectionEvent.sampleCategories.parent ===
                    "Fluids and Secretions"
                ) {
                  sampleTypes["Fluids and Secretions"] += 1;
                } else if (
                  collectionEvent.sampleCategories.name ===
                    "Genetic material" ||
                  collectionEvent.sampleCategories.parent === "Genetic material"
                ) {
                  sampleTypes["Genetic material"] += 1;
                } else if (
                  collectionEvent.sampleCategories.name.startsWith("Tissue") ||
                  collectionEvent.sampleCategories.parent.startsWith("Tissue")
                ) {
                  sampleTypes["Tissue"] += 1;
                }
              }
              return sampleTypes;
            },
            sampleTypes
          );
        }
        return sampleTypes;
      }, this.sampleTypeCounts);

      const newsItemsSettings = resp._settings.filter(
        s => s.key === "newsItems"
      );
      this.newsItems = newsItemsSettings[0]
        ? JSON.parse(newsItemsSettings[0].value)
        : [];

      const recentlyAddedSettings = resp._settings.filter(
        s => s.key === "recentlyAdded"
      );
      this.recentlyAdded = recentlyAddedSettings[0]
        ? JSON.parse(recentlyAddedSettings[0].value)
        : [];
    },
  },
  mounted: function () {
    this.load();
  },
};
</script>
