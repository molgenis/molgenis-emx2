<template>
  <div class="container bg-white">
    <ResourceHeader
      header-css="bg-danger text-white"
      :resource="databank"
      table-name="Databanks"
    />
    <MessageError v-if="graphqlError">
      {{ graphqlError }}
    </MessageError>
    <hr class="border-danger">
    <div class="row">
      <div class="col">
        <h6 v-if="databank.datasource">
          Part of datasource
        </h6>
        <DatasourceList :datasources="[databank.datasource]" />
        <h6>Population</h6>
        <OntologyTerms color="danger" :terms="databank.population" />
        <h6>Inclusion criteria</h6>
        <OntologyTerms color="danger" :terms="databank.inclusionCriteria" />
        <h6>Topics</h6>
        <p>{{ databank.topics ? databank.topics : "N/A" }}</p>
        <h6>Number of participants:</h6>
        <p>{{ databank.noParticipants ? databank.noParticipants : "N/A" }}</p>
        <hr class="border-danger">
        <h6>Orginator</h6>
        <p>{{ databank.originator ? databank.originator : "N/A" }}</p>
        <h6>Record prompt:</h6>
        <p>{{ databank.recordPrompt ? databank.recordPrompt : "N/A" }}</p>
        <h6>Start/End year</h6>
        <p>
          {{ databank.startYear ? databank.startYear : "N/A" }} -
          {{ databank.endYear ? databank.endYear : "N/A" }}
        </p>
        <h6>Update frequency</h6>
        <p>{{ databank.updates ? databank.updates : "N/A" }}</p>
        <h6>Lag time</h6>
        <p>{{ databank.lagtime ? databank.lagtime : "N/A" }}</p>
        <h6>Collection events</h6>
        <p>
          {{ databank.collectionEvents ? databank.collectionEvents : "N/A" }}
        </p>
        <hr class="border-danger">
        <Conditions :resource="databank" />
      </div>
      <div class="col">
        <ResourceContext :resource="databank" />
      </div>
    </div>
  </div>
</template>

<script>
import Conditions from '@/components/catalogue/Conditions.vue'
import DatasourceList from '@/components/catalogue/DatasourceList.vue'
import {MessageError} from '@/components/ui/index.js'
import OntologyTerms from '@/components/catalogue/OntologyTerms.vue'
import {request} from 'graphql-request'
import ResourceContext from '@/components/catalogue/ResourceContext.vue'
import ResourceHeader from '@/components/catalogue/ResourceHeader.vue'

export default {
  components: {
    Conditions,
    DatasourceList,
    MessageError,
    OntologyTerms,
    ResourceContext,
    ResourceHeader,
  },
  props: {
    acronym: String,
  },
  data() {
    return {
      databank: {},
      graphqlError: null,
      tab: 'Description',
      version: null,
    }
  },
  watch: {
    acronym() {
      this.reload()
    },
  },
  created() {
    this.reload()
  },
  methods: {
    reload() {
      request(
        'graphql',
        'query Databanks($acronym:String){Databanks(filter:{acronym:{equals:[$acronym]}}){name,logo{url},acronym,contributors{contact{name},contributionType{name}},contact{name,email},datasource{acronym,name}, population{name},noParticipants,conditions{name,ontologyTermURI,code,definition},inclusionCriteria{name,definition},updateFrequency{name}, startYear,endYear, type{name,definition,ontologyTermURI},provider{acronym,name}, description,homepage,recordPrompt, lagTime, releases{resource{acronym},version},documentation{name,file{url},url},publications{doi,title,authors,year,journal,volume,number,pagination,publisher,school,abstract},networks{acronym,name},acknowledgements,funding}}',
        {
          acronym: this.acronym,
        },
      )
        .then((data) => {
          this.databank = data.Databanks[0]
          if (this.databank.releases) {
            this.version = this.databank.releases[
              this.databank.releases.length - 1
            ].version
          }
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally(() => {
          this.loading = false
        })
    },
  },
}
</script>
