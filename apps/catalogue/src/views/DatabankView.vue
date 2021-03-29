     <template>
  <div class="container bg-white">
    <ResourceHeader
      :resource="databank"
      headerCss="bg-danger text-white"
      table-name="Databanks"
    />
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <hr class="border-danger" />
    <div class="row">
      <div class="col">
        <h6 v-if="databank.datasource">Part of datasource</h6>
        <DatasourceList :datasources="[databank.datasource]" />
        <h6>Population</h6>
        <OntologyTerms :terms="databank.population" color="danger" />
        <h6>Contents</h6>
        <OntologyTerms :terms="databank.keywords" color="danger" />
        <h6 v-if="databank.noParticipants">Number of participants:</h6>
        <p v-if="databank.noParticipants">{{ databank.noParticipants }}</p>
        <hr class="border-danger" />
        <h6>Orginator</h6>
        <p>{{ databank.originator ? databank.originator : "N/A" }}</p>
        <h6>
          Record prompt:
          <OntologyTerms
            :terms="databank.recordPrompt"
            color="danger"
            :inline="true"
          />
        </h6>
        <p>
          {{ databank.recordPromptDescription }}
        </p>
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
        <hr class="border-danger" />
        <Conditions :resource="databank" />
      </div>
      <div class="col">
        <h6>Data access providers</h6>
        <InstitutionList :institutions="databank.provider" />
        <h6>Documentation</h6>
        <DocumentationList :documentation="databank.documentation" />
        <h6 v-if="databank.releases">Data releases</h6>
        <ReleasesList v-if="databank.releases" :releases="databank.releases" />
        <h6 v-if="databank.cdms">Common data models</h6>
        <ReleasesList v-if="databank.cdms" :releases="databank.cdms" />
        <PublicationList
          v-if="databank.publications"
          :publications="databank.publications"
        />
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

export default {
  components: {
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
      databank: {},
      version: null,
      tab: "Description",
    };
  },
  methods: {
    reload() {
      request(
        "graphql",
        `query Databanks($acronym:String){Databanks(filter:{acronym:{equals:[$acronym]}}){name,originator,logo{url},keywords{name,definition},acronym,contributors{contact{name},contributionType{name}},contact{name,email},datasource{acronym,name}, population{name},noParticipants,conditionsDescription,conditions{name,ontologyTermURI,code,definition},inclusionCriteria{name,definition},updateFrequency{name}, startYear,endYear, type{name,definition,ontologyTermURI},provider{acronym,name}, description,homepage,recordPrompt{name,definition},recordPromptDescription, lagTime, releases{resource{acronym},version},cdms{resource{acronym},version},documentation{name,file{url},url},publications{doi,title,authors,year,journal,volume,number,pagination,publisher,school,abstract},networks{acronym,name},acknowledgements,funding}}`,
        {
          acronym: this.acronym,
        }
      )
        .then((data) => {
          this.databank = data.Databanks[0];
          if (this.databank.releases) {
            this.version = this.databank.releases[
              this.databank.releases.length - 1
            ].version;
          }
        })
        .catch((error) => {
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
