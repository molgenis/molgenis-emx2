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
  async nuxtServerInit({ dispatch, commit }, context) {
    if (process.server) {
      const { req, res, beforeNuxtRender } = context;
      const path = context.req.url;
      const schema = path.split("/").filter((i) => i !== "")[0];
      commit("setSchema", schema);
      await dispatch("fetchSession");
    }
  },
  async fetchSession(context) {
    // console.log("fetchSession for schema: " + context.state.schema);
    const query =
      '{_session{email,roles},_settings(keys: ["menu", "page.", "cssURL", "logoURL", "isOidcEnabled"]){key,value},_manifest{ImplementationVersion,SpecificationVersion,DatabaseVersion}}';
    const sessionUrl = context.state.schema ? context.state.schema + "/graphql" : "apps/central/graphql";
    const resp = await this.$axios({
      url: sessionUrl,
      method: "post",
      data: { query },
    }).catch((e) => console.error(e));
    const settings = resp.data.data._settings;
    const menuSetting = settings.find((s) => s.key === "menu");
    if (menuSetting) {
      const menuString = menuSetting.value;
      const menuItems = JSON.parse(menuString).map((menuItem) => {
        // Strip the added ssr context from the menu.href if relative
        const separator = menuItem.href.startsWith("/") ? "" : "/";
        if (menuItem.href) {
          menuItem.href = context.state.schema
            ? `/${context.state.schema}${separator}${menuItem.href}`
            : `${separator}${menuItem.href}`;
        }
        return menuItem;
      });
      context.commit("setMenu", menuItems);
    }
  },

};
