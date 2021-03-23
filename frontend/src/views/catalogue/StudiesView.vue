<template>
    <div class="container bg-white">
        <ResourceHeader
            header-css="bg-success text-white"
            :resource="study"
            table-name="Studies"
        />
        <MessageError v-if="graphqlError">
            {{ graphqlError }}
        </MessageError>
        <div class="row">
            <div class="col">
                <h6>Datasources involved</h6>
                <DatasourceList :datasources="study.datasources" />
                <h6>Databanks involved</h6>
                <DatabankList :databanks="study.databanks" />
                <h6>Funding</h6>
                <p>{{ study.funding ? study.funding : "N/A" }}</p>
            </div>
            <div class="col">
                <ResourceContext :resource="study" />
            </div>
        </div>
    </div>
</template>
<script>
import { request } from "graphql-request";

import { MessageError } from "@/components/ui/index.js";
import DatabankList from "@/components/catalogue/DatabankList.vue";
import DatasourceList from "@/components/catalogue/DatasourceList.vue";
import ResourceHeader from "@/components/catalogue/ResourceHeader.vue";
import ResourceContext from "@/components/catalogue/ResourceContext.vue";

export default {
  components: {
    ResourceContext,
    DatasourceList,
    DatabankList,
    MessageError,
    ResourceHeader,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      graphqlError: null,
      study: null,
    };
  },
  created() {
    this.reload();
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Studies($acronym:String){Studies(filter:{acronym:{equals:[$acronym]}})
        {acronym,provider{acronym,name},description,homepage,contact{name,email},name,partners{institution{acronym,name}},networks{acronym,name},databanks{acronym,name}}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.study = data.Studies[0];
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
