<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="cohort"
      headerCss="bg-primary text-white"
      table-name="Cohorts"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <hr class="border-primary" />
    <div class="row">
      <div class="col-8">
        <h6>Start/End year</h6>
        <p>
          {{ cohort.startYear ? cohort.startYear : "N/A" }} -
          {{ cohort.endYear ? cohort.endYear : "N/A" }}
        </p>
        <h6>Population</h6>
        <OntologyTerms :terms="cohort.population" color="primary" />
        <h6>Available data</h6>
        <OntologyTerms :terms="cohort.dataCategories" color="primary" />
        <h6>Available samples</h6>
        <OntologyTerms :terms="cohort.sampleCategories" color="primary" />
        <h6 v-if="cohort.noParticipants">Number of participants:</h6>
        <p v-if="cohort.noParticipants">{{ cohort.noParticipants }}</p>
        <h6>Collection events</h6>
        <div v-if="!cohort.collectionEvents">N/A</div>
        <CollectionEventsList
          v-else
          :collection-events="cohort.collectionEvents"
        />
        <h6>Subcohorts</h6>
        <div v-if="!cohort.subcohorts">N/A</div>
        <SubcohortList v-else :subcohorts="cohort.subcohorts" color="primary" />
        <Conditions :resource="cohort" color="primary" />
        <h6>Linkage options</h6>
        <div v-if="!cohort.linkageOptions">N/A</div>
        <div v-else>{{ cohort.linkageOptions }}</div>
        <h6>Marker publication</h6>
        <div v-if="!cohort.publication">N/A</div>
        <div v-else>{{ cohort.publication }}</div>
      </div>
      <div class="col-4">
        <h6>Data access provider(s):</h6>
        <InstitutionList :institutions="cohort.institution" />
        <h6>Contributors:</h6>
        <ContributorList :contributors="cohort.contributors" color="primary" />
        <h6>Partners:</h6>
        <PartnersList :partners="cohort.partners" color="primary" />
        <h6>Networks:</h6>
        <NetworkList :networks="cohort.networks" color="primary" />
        <h6>Documentation:</h6>
        <DocumentationList :documentation="cohort.documentation" />
        <h6 v-if="cohort.releases">Data releases</h6>
        <ReleasesList v-if="cohort.releases" :releases="cohort.releases" />
        <h6 v-if="cohort.cdms">Common data models</h6>
        <ReleasesList v-if="cohort.cdms" :releases="cohort.cdms" />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import {
  MessageError,
  ReadMore,
  InputSelect,
  NavTabs,
} from "@mswertz/emx2-styleguide";
import VariablesList from "../components/VariablesList";
import OntologyTerms from "../components/OntologyTerms";
import PublicationList from "../components/PublicationList";
import ResourceHeader from "../components/ResourceHeader";
import InstitutionList from "../components/InstitutionList";
import ReleasesList from "../components/ReleasesList";
import ContactList from "../components/ContactList";
import DocumentationList from "../components/DocumentationList";
import DatasourceList from "../components/DatasourceList";
import NetworkList from "../components/NetworkList";
import Conditions from "../components/Conditions";
import ContributorList from "../components/ContributorList";
import ResourceContext from "../components/ResourceContext";
import PartnerInList from "../components/PartnerInList";
import PartnersList from "../components/PartnersList";
import CollectionEventsList from "../components/CollectionEventsList";
import SubcohortList from "../components/SubcohortList";

export default {
  components: {
    SubcohortList,
    CollectionEventsList,
    PartnersList,
    PartnerInList,
    ResourceContext,
    NetworkList,
    DatasourceList,
    ContactList,
    ReleasesList,
    ResourceHeader,
    InstitutionList,
    PublicationList,
    OntologyTerms,
    MessageError,
    ReadMore,
    VariablesList,
    NavTabs,
    InputSelect,
    DocumentationList,
    Conditions,
    ContributorList,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      graphqlError: null,
      cohort: {},
      version: null,
      tab: "Description",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `
          query Cohorts($acronym: String) {
            Cohorts(filter: { acronym: { equals: [$acronym] } }) {
              name
              logo {
                url
              }
              keywords {
                name
                definition
              }
              sampleCategories {
                name
              }
              dataCategories {
                name
              }
              acronym
              contributors {
                contact {
                  name
                }
                contributionType {
                  name
                }
              }
              partners {
                institution {
                  name,acronym
                }
                role {
                  name
                }
              }
              contact {
                name
                email
              }
              population {
                name
              }
              linkageOptions
              noParticipants
              conditionsDescription
              conditions {
                name
                ontologyTermURI
                code
                definition
              }
              inclusionCriteria {
                name
                definition
              }
              startYear
              endYear
              type {
                name
                definition
                ontologyTermURI
              }
              institution {
                acronym
                name
              }
              description
              homepage
              releases {
                resource {
                  acronym
                }
                version
              }

              documentation {
                name
                file {
                  url
                }
                url
              }
              networks {
                acronym
                name
              }
              acknowledgements
              fundingStatement
              publication
              collectionEvents {
                name, startYear, endYear, ageMin{name}, ageMax{name}
              }
              subcohorts {
                name,noParticipants,ageCategories{name},disease{name},geographicRegion{name}
              }
            }
          }
        `,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.cohort = data.Cohorts[0];
          if (this.cohort.releases) {
            this.version =
              this.cohort.releases[this.cohort.releases.length - 1].version;
          }
        })
        .catch((error) => {
          console.log(error);
          this.graphqlError = error.response.errors[0].message;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
  watch: {
    acronym() {
      this.reload();
    },
  },
};
</script>
