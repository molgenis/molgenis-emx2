export const state = () => ({
  schema: null,
  menu: [],
  counts: {},
});

export const mutations = {
  setSchema(state, schema) {
    state.schema = schema;
  },
  setMenu(state, menu) {
    state.menu = menu;
  },
  setCounts(state, counts) {
    state.counts = counts;
  },
};

export const actions = {
  async fetchSession(context) {
    const query =
      '{_session{email,roles},_settings(keys: ["menu", "page.", "cssURL", "logoURL", "isOidcEnabled"]){key,value},_manifest{ImplementationVersion,SpecificationVersion,DatabaseVersion}}';
    const resp = await this.$axios({
      url: context.state.schema
        ? context.state.schema + "/graphql"
        : "apps/central/graphql",
      method: "post",
      data: { query },
    }).catch((e) => console.error(e));
    const settings = resp.data.data._settings;
    const menuSetting = settings.find((s) => s.key === "menu");
    if (menuSetting) {
      const menuString = menuSetting.value;
      const menu = JSON.parse(menuString);
      context.commit("setMenu", menu);
    }
  },

  async fetchCounts(context) {
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
      VariableMappings_agg{count}, TableMappings_agg{count}}
    `;
    const url = context.state.schema + "/graphql";
    const resp = await this.$axios({
      url: url,
      method: "post",
      data: { query },
    }).catch((e) => {
      console.error(e);
      console.error(
        "Unable to fetch catalog count, make use this current schema support the catalog model"
      );
    });
    if (resp && resp.data && resp.data.data) {
      const counts = resp.data.data;
      context.commit("setCounts", {
        institutions: counts.Institutions_agg.count,
        cohorts: counts.Cohorts_agg.count,
        databanks: counts.Databanks_agg.count,
        datasources: counts.Datasources_agg.count,
        networks: counts.Networks_agg.count,
       
        models: counts.Models_agg.count,
        studies: counts.Studies_agg.count,
        
        // variables: counts.Variables_agg.count,
        // variableMappings: counts.VariableMappings_agg.count,
        // tableMappings: counts.TableMappings_agg.count,
      });
    }
  },
};
