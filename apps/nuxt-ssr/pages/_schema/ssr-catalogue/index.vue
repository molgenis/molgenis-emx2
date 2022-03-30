<template>
  <div>
    <div class="container">
      <h1>
        <span v-if="$route.params.schema == 'Minerva'">
          Proof-of-concept catalogue tool for MINERVA metadata pilot</span
        >
        <span v-else>Data catalogue</span>
      </h1>
      <p v-if="$route.params.schema == 'Minerva'" class="text-danger">
        Disclaimer: contents not for public disclosure.
      </p>
      <p>
        Browse and manage metadata for data resources, such as cohorts,
        registries, biobanks, and multi-center collaborations thereof such as
        networks, common data models and studies.
      </p>
      <h2>Metadata on data collections</h2>
      <div class="card-columns">
        <count-card
          class="bg-dark text-white"
          :count="counts.institutions"
          label="Institutions"
          :to="{ path: this.routePath + 'Institutions' }"
        >
          Contributors to the catalogue such as universities, companies, medical
          centres and research institutes
        </count-card>

        <count-card
          class="bg-danger text-white"
          :count="counts.networks"
          label="Networks"
          :to="{ path: this.routePath + 'Networks' }"
        >
          Collaborations of multiple institutions
        </count-card>

        <count-card
          class="bg-primary text-white"
          :count="counts.datasources"
          label="Data sources"
          :to="{ path: this.routePath + 'Datasources' }"
        >
          Collections of data banks covering the same population
        </count-card>

        <count-card
          class="bg-primary text-white"
          :count="counts.cohorts"
          label="Cohorts"
          :to="{ path: this.routePath + 'Cohorts' }"
        >
          Systematic observations of large groups of individuals over time.
        </count-card>

        <count-card
          class="bg-info text-white"
          :count="counts.databanks"
          label="Data banks"
          :to="{ path: this.routePath + 'Databanks' }"
        >
          Data collections such as registries or biobanks
        </count-card>

        <count-card
          class="bg-success text-white"
          :count="counts.studies"
          label="Studies"
          :to="{ path: this.routePath + 'Studies' }"
        >
          Collaborations of multiple institutions, addressing research questions
          using data sources and/or data banks
        </count-card>
      </div>

      <h2>Browse data definitions</h2>
      <div class="card-columns">
        <div class="card card-body border border-dark rounded card-height">
          <h3>Collected data dictionaries</h3>
          <div class="text-left">
            Data dictionaries of collected data in databanks and/or cohorts.
            <ul>
              <li>
                <RouterLink :to="{ path: this.routePath + 'SourceDataDictionaries' }">
                  Source Data dictionaries ({{ counts.sourceDataDictionaries }})
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: this.routePath + 'SourceTables' }"> Source Tables ({{ counts.sourceTables }}) </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: this.routePath + 'SourceVariables' }">
                  Source Variables ({{ counts.sourceVariables }})
                </RouterLink>
              </li>
            </ul>
          </div>
        </div>

        <div class="card card-body border border-dark rounded card-height">
          <h3>Common data models</h3>
          <div class="text-left">
            Data dictionaries of standards for integrated analysis
            <ul>
              <li>
                <RouterLink :to="{ path: this.routePath + 'TargetDataDictionaries'}">
                  Target Data dictionaries ({{counts.targetDataDictionaries}})
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: this.routePath + 'TargetTables'}"> Target Tables ({{counts.targetTables}}) </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: this.routePath + 'TargetVariables'}" >
                  Target Variables ({{counts.targetVariables}})
                </RouterLink>
              </li>
            </ul>
          </div>
        </div>

        <div class="card card-body border border-dark card-height">
          <h3>Data model mappings</h3>
          <div class="text-left">
            Mappings between collected data dictionaries and standard models
            <ul>
              <li>
                <RouterLink :to="{ path: this.routePath + 'VariableMappings'}">
                  Variable mappings ({{ counts.variableMappings }})
                </RouterLink>
              </li>
              <li>
                <RouterLink :to="{ path: this.routePath + 'TableMappings'}"> Table mappings ({{ counts.tableMappings }}) </RouterLink>
              </li>
            </ul>
          </div>
        </div>
      </div>
      <p>
        This catalogue software has been made possible by contributions from
        H2020 EUCAN-connect, LifeCycle, Longitools and ATHLETE as well as IMI
        Conception and EMA Minerva.
      </p>
    </div>
  </div>
</template>

<style scoped>
.btn .badge {
  position: absolute;
  top: 5px;
  right: 5px;
}

.card-height {
  min-height: 200px;
}
</style>

<script>
import CountCard from "../../../components/CountCard.vue";
const query = `query {
      Institutions_agg{count},
      Studies_agg{count},
      Cohorts_agg{count},Databanks_agg{count},
      Databanks_agg{count}
      Datasources_agg{count},Networks_agg{count},
      Networks_agg{count}
      SourceTables_agg{count},
      TargetTables_agg{count},
      Models_agg{count},
      Studies_agg{count},
      SourceDataDictionaries_agg{count},
      TargetDataDictionaries_agg{count},
      SourceVariables_agg{count},
      TargetVariables_agg{count},
      VariableMappings_agg{count},
      TableMappings_agg{count}
    }`;
export default {
  name: "SSRCatalogue",
  components: { CountCard },
  async asyncData({ $axios, store, params }) {
    store.dispatch("fetchSession");
    const resp = await $axios
      .post(params.schema + "/graphql", { query })
      .catch((e) => {
        console.log(
          "Unable to fetch catalog count, make sure the current schema supports the catalog model"
        );
        console.log(e);
      });
    if (resp && resp.data && resp.data.data) {
      const countsRespData = resp.data.data;
      const counts = {
        institutions: countsRespData.Institutions_agg.count,
        databanks: countsRespData.Databanks_agg.count,
        cohorts: countsRespData.Cohorts_agg.count,
        networks: countsRespData.Networks_agg.count,
        datasources: countsRespData.Datasources_agg.count,
        sourceDataDictionaries: countsRespData.SourceDataDictionaries_agg.count,
        sourceTables: countsRespData.SourceTables_agg.count,
        sourceVariables: countsRespData.SourceVariables_agg.count,
        models: countsRespData.Models_agg.count,
        targetDataDictionaries: countsRespData.TargetDataDictionaries_agg.count,
        targetTables: countsRespData.TargetTables_agg.count,
        targetVariables: countsRespData.TargetVariables_agg.count,
        variableMappings: countsRespData.VariableMappings_agg.count,
        tableMappings: countsRespData.TableMappings_agg.count,
        studies: countsRespData.Studies_agg.count,
      };
      return resp ? { counts } : null;
    }
  },
  computed: {
    schema() {
      return this.$store.state.schema;
    },
    routePath() {
      return this.$route.path.endsWith("/")
        ? this.$route.path
        : this.$route.path + "/";
    },
  },
};
</script>
