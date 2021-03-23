<template>
    <div class="container bg-white">
        <ResourceHeader
            header-css="bg-secondary text-white"
            :resource="model"
            table-name="Models"
        />
        <MessageError v-if="graphqlError">
            {{ graphqlError }}
        </MessageError>
        <div class="row">
            <div class="col">
                <h6>Coordinator</h6>
                <p>{{ model.provider ? model.provider.name : "N/A" }}</p>
                <h6>Institutions</h6>
                <PartnersList :institutions="model.partners" />
                <h6>Networks involved</h6>
                <NetworkList :networks="model.networks" />
                <h6>Databanks involved</h6>
                <DatabankList :databanks="model.databanks" />
                <h6>Funding</h6>
                <p>{{ model.funding ? model.funding : "N/A" }}</p>
            </div>
            <div class="col">
                <ResourceContext :resource="model" />
            </div>
        </div>
    </div>
</template>

<script>
import { request } from "graphql-request";
import {
  MessageError,
} from "@/components/ui/index.js";
import DatabankList from "@/components/catalogue/DatabankList.vue"
import NetworkList from "@/components/catalogue/NetworkList.vue"
import PartnersList from "@/components/catalogue/PartnersList.vue"
import ResourceHeader from "@/components/catalogue/ResourceHeader.vue"
import ResourceContext from "@/components/catalogue/ResourceContext.vue"

export default {
  components: {
    PartnersList,
    NetworkList,
    DatabankList,
    MessageError,
    ResourceHeader,
    ResourceContext,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      version: null,
      graphqlError: null,
      model: {},
    };
  },
  watch: {
    modelAcronym() {
      this.reload();
    },
  },
  created() {
    this.reload();
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Models($acronym:String){Models(filter:{acronym:{equals:[$acronym]}}){name,acronym,type{name},provider{acronym,name}, description,homepage, partners{institution{acronym,name,country{name}}},releases{resource{acronym,name},version}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.model = data.Models[0];
        })
        .catch((error) => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>
