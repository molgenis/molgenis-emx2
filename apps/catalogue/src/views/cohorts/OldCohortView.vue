<template>
  <Spinner v-if="loading" />
  <div v-else class="container bg-white">
    <ResourceHeader
      :resource="cohort"
      headerCss="bg-primary text-white"
      table-name="Cohorts"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <hr class="border-primary" />
    <div class="row">
      <div class="col-7">
        <h4>Summary</h4>
        <h6>Design</h6>
        <OntologyTerms :terms="[cohort.design]" color="primary" />
        <h6>Collection type</h6>
        <OntologyTerms :terms="cohort.collectionType" color="primary" />
        <h6>Start/End year</h6>
        <p>
          {{ cohort.startYear ? cohort.startYear : "N/A" }} -
          {{ cohort.endYear ? cohort.endYear : "N/A" }}
        </p>
        <h6>Countries</h6>
        <OntologyTerms :terms="cohort.countries" color="primary" />
        <h6>Regions</h6>
        <OntologyTerms :terms="cohort.regions" color="primary" />
        <h6 v-if="cohort.numberOfParticipants">Number of participants:</h6>
        <p v-if="cohort.numberOfParticipants">
          {{ cohort.numberOfParticipants }}
        </p>
        <h6>Linkage options</h6>
        <p>{{ cohort.linkageOptions ? cohort.linkageOptions : "N/A" }}</p>
        <h6>Design paper</h6>
        <p>{{ cohort.designPaper ? cohort.designPaper.title : "N/A" }}</p>
      </div>
      <div class="col-5 border-left border-primary">
        <h4>Organisation</h4>
        <h6>Data access provider(s):</h6>
        <InstitutionList :institutions="cohort.institution" />
        <h6>Contributors:</h6>
        <ContributorList :contributors="cohort.contributors" color="primary" />
        <h6>Partners:</h6>
        <PartnersList :partners="cohort.partners" color="primary" />
        <h6>Networks:</h6>
        <NetworkList :networks="cohort.networks" color="primary" />
        <h6 v-if="cohort.releases">Data releases</h6>
        <ReleasesList v-if="cohort.releases" :releases="cohort.releases" />
        <h6 v-if="cohort.cdms">Common data models</h6>
        <ReleasesList v-if="cohort.cdms" :releases="cohort.cdms" />
        <h6>External identifiers</h6>
        <p>
          {{ cohort.externalIdentifiers ? cohort.externalIdentifiers : "N/A" }}
        </p>
      </div>
    </div>
    <hr class="border-primary" />
    <div class="row">
      <div class="col">
        <h4>Contents</h4>
        <h6>Subcohorts</h6>
        <div v-if="!cohort.subcohorts">N/A</div>
        <SubcohortList v-else :subcohorts="cohort.subcohorts" color="primary" />
        <h6>Collection events</h6>
        <div v-if="!cohort.collectionEvents">N/A</div>
        <CollectionEventsList
          v-else
          :collection-events="cohort.collectionEvents"
        />
        <h6>Documentation:</h6>
        <DocumentationList :documentation="cohort.documentation" />
      </div>
    </div>
    <hr class="border-primary" />
    <div class="row">
      <div class="col">
        <h4>Conditions of use</h4>
        <Conditions :resource="cohort" color="primary" />
      </div>
    </div>
  </div>
</template>

<script>
import { request } from "graphql-request";
import {
  MessageError,
  Spinner,
} from "molgenis-components";

import OntologyTerms from "../../components/OntologyTerms";
import ResourceHeader from "../../components/ResourceHeader";
import InstitutionList from "../../components/InstitutionList";
import ReleasesList from "../../components/ReleasesList";
import DocumentationList from "../../components/DocumentationList";
import NetworkList from "../../components/NetworkList";
import Conditions from "../../components/Conditions";
import ContributorList from "../../components/ContributorList";
import PartnersList from "../../components/PartnersList";
import CollectionEventsList from "../../components/CollectionEventsList";
import SubcohortList from "../../components/SubcohortList";

export default {
  components: {
    SubcohortList,
    CollectionEventsList,
    PartnersList,
    NetworkList,
    ReleasesList,
    ResourceHeader,
    InstitutionList,
    OntologyTerms,
    MessageError,
    DocumentationList,
    Conditions,
    ContributorList,
    Spinner,
  },
  props: {
    pid: String,
  },
  data() {
    return {
      loading: false,
      graphqlError: null,
      cohort: {},
      version: null,
      tab: "Description",
    };
  },
  methods: {
    reload() {
      this.loading = true;
      request(
        "graphql",
        `
          query Cohorts($pid: String) {
            Cohorts(filter: { pid: { equals: [$pid] } }) {
              name
              logo {
                url
              }
              design{name}
              collectionType{name}
              keywords
              pid
              externalIdentifiers
              contributors {
                contact {
                  firstName
                  surname
                }
                contributionType {
                  name
                }
              }
              partners {
                institution {
                  name,pid
                }
                role {
                  name
                }
              }
              countries {
                name
              }
              regions {
                name
              }
              linkageOptions
              numberOfParticipants
              dataAccessConditionsDescription
              dataAccessConditions {
                name
                ontologyTermURI
                code
                definition
              }
              dataUseConditions {
                name
                ontologyTermURI
                code
                definition
              }
              dataAccessFee
              startYear
              endYear
              type {
                name
                definition
                ontologyTermURI
              }
              institution {
                pid
                name
              }
              description
              website
              documentation {
                name
                file {
                  url
                }
                url
              }
              networks {
                pid
                name
              }
              acknowledgements
              fundingStatement
              designPaper { 
                doi 
                title
              }
              collectionEvents {
                name, description, startYear{name}, endYear{name},numberOfParticipants, ageGroups{name}, dataCategories{name},sampleCategories{name},areasOfInformation{name},subcohorts{name}
              }
              subcohorts {
                name, description, numberOfParticipants,ageGroups{name},mainMedicalCondition{name},countries{name},regions{name},inclusionCriteria,supplementaryInformation
              }
            }
          }
        `,
        {
          pid: this.pid,
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
          this.graphqlError = error;
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
    pid() {
      this.reload();
    },
  },
};
</script>
