<template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="study"
      headerCss="bg-success text-white"
      table-name="Studies" />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div class="row">
      <div class="col">
        <h6>Study ID</h6>
        <p>{{ study.pid }}</p>
        <h6>Study title</h6>
        <p>{{ study.name }}</p>
        <h6>Keywords</h6>
        <OntologyTerms color="success" :terms="study.keywords" />
        <h6>Datasources involved</h6>
        <DatasourceList :datasources="study.datasources" />
        <h6 v-if="study.databanks">Databanks involved</h6>
        <DatabankList v-if="study.databanks" :databanks="study.databanks" />
        <h6>Networks involved</h6>
        <NetworkList :networks="study.networks" />
        <h6 v-if="study.funding">Funding</h6>
        <p v-if="study.funding">{{ study.funding ? study.funding : "N/A" }}</p>
        <h6>Results <i class="fa fa-caret-down"></i></h6>
        <h6>
          Publications
          <i class="fa fa-caret-down"></i>
        </h6>
        <h6>
          Quality assesment
          <i class="fa fa-caret-down"></i>
        </h6>
      </div>
      <div class="col">
        <ResourceContext :resource="study" />
      </div>
    </div>
  </div>
</template>
<script>
import { request } from "graphql-request";
import { MessageError } from "molgenis-components";
import DatabankList from "../components/DatabankList.vue";
import DatasourceList from "../components/DatasourceList.vue";
import ResourceHeader from "../components/ResourceHeader.vue";
import ResourceContext from "../components/ResourceContext.vue";
import NetworkList from "../components/NetworkList.vue";
import OntologyTerms from "../components/OntologyTerms.vue";

export default {
  components: {
    OntologyTerms,
    ResourceContext,
    DatasourceList,
    DatabankList,
    MessageError,
    ResourceHeader,
    NetworkList,
  },
  props: {
    pid: String,
  },
  data() {
    return {
      graphqlError: null,
      study: null,
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Studies($pid:String){Studies(filter:{pid:{equals:[$pid]}})
        {pid,institution{pid,name},keywords{name,definition}description,homepage,contact{name,email},name,partners{institution{pid,name}},networks{pid,name},datasources{pid,name},networks{pid,name},databanks{pid,name},documentation{name,url}}}`,
        {
          pid: this.pid,
        }
      )
        .then(data => {
          this.study = data.Studies[0];
        })
        .catch(error => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message;
          else this.graphqlError = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
  created() {
    this.reload();
  },
};
</script>
