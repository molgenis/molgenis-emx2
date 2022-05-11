<template>
  <div v-if="networkDetails" class="container">
    <div>
      <p>
        Welcome to the home page for the {{ network }} network. Use the 'view
        variables' and 'view cohorts' buttons to view details on
        cohorts/variables in the network
      </p>
    </div>
    <grid-block>
      <page-header
        :title="networkDetails.pid"
        :subTitle="
          networkDetails.institution ? networkDetails.institution[0].name : null
        "
        :logoUrl="networkDetails.logo ? networkDetails.logo.url : null"
      />
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
    <grid-block>
      <links-list
        :isHorizontal="true"
        :items="[
          ...(networkDetails.website
            ? [{ label: 'Website:', href: networkDetails.website }]
            : []),
        ]"
      ></links-list>
    </grid-block>

    <key-value-block
      :items="[{ label: 'Description', value: networkDetails.description }]"
    ></key-value-block>

    <key-value-block
      v-if="networkDetails.fundingStatement"
      :items="[{ label: 'Funding', value: networkDetails.fundingStatement }]"
    ></key-value-block>
  </div>
</template>

<script>
import { TableMixin } from "@mswertz/emx2-styleguide";

import { request } from "graphql-request";

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
        console.log("haat");
        const result = await request(
          "graphql",
          `{Networks(filter: { pid: { equals: "${this.network}" } }){
            pid
            name
            localName
            acronym
            website
            description
            contributors {
               contact { firstName, surname}
               contributionType{name}
            }
            institution{
              name
            }
            logo {
              url
            }
            startYear
            endYear
            fundingStatement
            acknowledgements
            partners {
              institution {
                name
              }
            department
            }
          }}`
        ).catch((error) => console.log(error));
        this.networkDetails = result.Networks[0];
        console.log(JSON.stringify(this.networkDetails));
        this.$forceUpdate();
      }
    },
  },
  mounted: async function () {
    this.fetchData();
  },
};
</script>
