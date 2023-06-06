<template>
  <div v-if="networkDetails" class="container">
    <grid-block>
      <page-header
        :title="networkDetails.id"
        :subTitle="
          networkDetails.leadOrganisation
            ? networkDetails.leadOrganisation.name
            : null
        "
        :logoUrl="networkDetails.logo ? networkDetails.logo.url : null" />
      <div>
        <RouterLink
          class="btn btn-primary ml-2 float-right"
          :to="{ name: 'NetworkCohorts', params: { network: network } }"
          >View cohorts
        </RouterLink>
        <RouterLink
          class="btn btn-primary float-right"
          :to="{ name: 'NetworkVariables', params: { network: network } }"
          >View variables
        </RouterLink>
      </div>
    </grid-block>
    <div>
      <p class="pl-2 pr-2">
        Welcome to the home page for the {{ network }} network. Use the 'view
        variables' and 'view cohorts' buttons to view details on
        cohorts/variables in the network
      </p>
    </div>
    <grid-block>
      <links-list
        :isHorizontal="true"
        :items="[
          ...(networkDetails.website
            ? [{ label: 'Website:', href: networkDetails.website }]
            : []),
        ]"></links-list>
    </grid-block>

    <key-value-block
      :items="[
        { label: 'Description', value: networkDetails.description },
      ]"></key-value-block>

    <key-value-block
      v-if="networkDetails.fundingStatement"
      :items="[
        { label: 'Funding', value: networkDetails.fundingStatement },
      ]"></key-value-block>
  </div>
</template>

<script>
import { request } from "graphql-request";

import {
  PageHeader,
  GridBlock,
  KeyValueBlock,
  LinksList,
} from "molgenis-components";

export default {
  props: {
    network: String,
  },
  data() {
    return {
      networkDetails: null,
    };
  },
  components: {
    GridBlock,
    LinksList,
    PageHeader,
    KeyValueBlock,
  },
  methods: {
    async fetchData() {
      if (this.network) {
        const result = await request(
          "graphql",
          `{Networks(filter: { id: { equals: "${this.network}" } }){
            id
            pid
            name
            acronym
            website
            description
            contacts {
               firstName,
               lastName,
               role{name}
            }
            leadOrganisation{
              name
            }
            logo {
              url
            }
            startYear
            endYear
            fundingStatement
            acknowledgements
            additionalOrganisations {
                name
            }
          }}`
        ).catch(error => console.log(error));
        this.networkDetails = result.Networks[0];
        this.$forceUpdate();
      }
    },
  },
  mounted: async function () {
    this.fetchData();
  },
};
</script>
