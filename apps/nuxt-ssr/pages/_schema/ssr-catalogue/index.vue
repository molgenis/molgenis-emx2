<template>
  <div>
    <div class="container pt-3">
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
      <div class="row justify-content-between">
        <count-card
          class="btn-dark"
          :count="counts.institutions"
          label="Institutions"
          :to="{ path: this.routePath + 'institutions' }"
        >
          Contributors to the catalogue such as universities, companies, medical
          centres and research institutes
        </count-card>

        <count-card
          class="btn-secondary"
          :count="counts.datasources"
          label="Data sources"
          :to="{ path: this.routePath + 'datasources' }"
        >
          Collections of data banks covering the same population
        </count-card>

        <count-card
          class="btn-info"
          :count="counts.databanks"
          label="Data banks"
          :to="{ path: this.routePath + 'databanks' }"
        >
          Data collections such as registries or biobanks
        </count-card>

        <count-card
          class="btn-primary"
          :count="counts.cohorts"
          label="Cohorts"
          :to="{ path: this.routePath + 'cohorts' }"
        >
          Systematic observations of large groups of individuals over time.
        </count-card>
      </div>
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
      Datasources_agg{count},Networks_agg{count},
      SourceTables_agg{count},TargetTables_agg{count},
      Models_agg{count},Studies_agg{count},
      SourceDataDictionaries_agg{count},
      TargetDataDictionaries_agg{count},
      SourceVariables_agg{count},
      TargetVariables_agg{count},
      VariableMappings_agg{count}, TableMappings_agg{count}}`;
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
        cohorts: countsRespData.Cohorts_agg.count,
        databanks: countsRespData.Databanks_agg.count,
        datasources: countsRespData.Datasources_agg.count,
        networks: countsRespData.Networks_agg.count,
        models: countsRespData.Models_agg.count,
        studies: countsRespData.Studies_agg.count,
        // variables: countsRespData.Variables_agg.count,
        // variableMappings: countsRespData.VariableMappings_agg.count,
        // tableMappings: countsRespData.TableMappings_agg.count,
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
