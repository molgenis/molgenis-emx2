export const state = () => ({
  schema: null,
  settings: [],
  session: null,
  manifest: null,
});

export const mutations = {
  setSchema(state, schema) {
    state.schema = schema;
  },
  setMenu(state, menu) {
    state.menu = menu;
  },
  setSettings(state, settings) {
    state.settings = settings;
  },
  setSession(state, session) {
    state.session = session;
  },
  setManifest(state, manifest) {
    state.manifest = manifest;
  },
};

export const getters = {
  menu(state) {
    const menuSetting = state.settings.find((s) => s.key === "menu");
    if (!menuSetting) {
      return [];
    }

    const menuItems = JSON.parse(menuSetting.value).map((menuItem) => {
      // Strip the added ssr context from the menu.href if relative
      const separator = menuItem.href.startsWith("/") ? "" : "/";
      if (menuItem.href) {
        menuItem.href = state.schema
          ? `/${state.schema}${separator}${menuItem.href}`
          : `${separator}${menuItem.href}`;
      }
      return menuItem;
    });

    const menuItemsForUser = menuItems.filter((menuItem) => {
      return state.session.roles.includes(menuItem.role);
    });

    return menuItemsForUser;
  },
  isOidcEnabled(state) {
    const oidcSetting = state.settings.find((s) => s.key === "isOidcEnabled");
    return oidcSetting && oidcSetting.value === "true";
  },
  logo(state) {
    const logoSetting = state.settings.find((s) => s.key === "logoURL");
    if (!logoSetting) {
      return undefined;
    }
    return logoSetting.value;
  },
};

export const actions = {
  async nuxtServerInit({ dispatch, commit }, context) {
    if (process.server) {
      const path = context.req.url;
      const schema = path.split("/").filter((i) => i !== "")[0];
      commit("setSchema", schema);
      await dispatch("fetchSession");
    }
  },
  async fetchSession(context) {
    const query = `{
        _session{ email, roles},
        _settings(keys: ["menu", "page.", "cssURL", "logoURL", "isOidcEnabled"]){ key, value },
        _manifest{ ImplementationVersion, SpecificationVersion, DatabaseVersion}
      }`;
    const sessionUrl = context.state.schema
      ? context.state.schema + "/graphql"
      : "apps/central/graphql";
    const resp = await this.$axios({
      url: sessionUrl,
      method: "post",
      data: { query },
    }).catch((e) => console.error(e));
    context.commit("setSession", resp.data.data._session);
    context.commit("setManifest", resp.data.data._manifest);
    context.commit("setSettings", resp.data.data._settings);
  },
  async signIn() {
    location.reload();
  },
  async signOut(context, { onSignOutFailed }) {
    const query = "mutation { signout { status } }";
    const signOutResp = await this.$axios
      .post("/api/graphql", { query })
      .catch((error) => onSignOutFailed("internal server error" + error));
    if (signOutResp.data.data.signout.status === "SUCCESS") {
      context.commit("setSession", {});
      if (location && location.reload) {
        location.reload();
      }
    } else {
      onSignOutFailed("sign out failed");
    }
  },
};
