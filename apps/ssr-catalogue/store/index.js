import Vue from 'vue'

export const state = () => ({
  menu: [],
  counts: {}
});

export const mutations = {
  setMenu(state, menu) {
    state.menu = menu
  },
  setCounts(state, counts) {
    state.counts = counts
  }
}

export const actions = {
  async fetchSession(context) {
    const query = '{_settings{key, value}}'
    const resp = await this.$axios({
      url: "graphql",
      method: "post",
      data: { query }
    }).catch(e => console.log(e));
    console.log(resp.data)
    const settings = resp.data.data._settings
    const menuString = settings.find(s => s.key === 'menu').value
    const menu = JSON.parse(menuString);
    context.commit("setMenu", menu);
  },

  async fetchCounts(context) {
    const query = `query {
      Institutions_agg{count},
      Cohorts_agg{count},
      Databanks_agg{count},
      Datasources_agg{count},
      Networks_agg{count},
      Tables_agg{count},
      Models_agg{count},
      Studies_agg{count},
      Releases_agg{count},
      Variables_agg{count},
      VariableMappings_agg{count},
      TableMappings_agg{count}}
    `
    const resp = await this.$axios({
      url: "graphql",
      method: "post",
      data: { query }
    }).catch(e => console.log(e));
    const counts = resp.data.data;
    context.commit("setCounts", {
      institutions: counts.Institutions_agg.count,
      cohorts: counts.Cohorts_agg.count,
      databanks: counts.Databanks_agg.count,
      datasources: counts.Datasources_agg.count,
      networks: counts.Networks_agg.count,
      tables: counts.Tables_agg.count,
      models: counts.Models_agg.count,
      studies: counts.Studies_agg.count,
      releases: counts.Releases_agg.count,
      variables: counts.Variables_agg.count,
      variableMappings: counts.VariableMappings_agg.count,
      tableMappings: counts.TableMappings_agg.count
    })

  }
};
