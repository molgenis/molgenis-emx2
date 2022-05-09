<template>
  <div style="background-color: #f4f4f4">
    <div style="min-height: calc(100vh - 70px)">
      <MolgenisMenu
        :logo="logoURLorDefault"
        active="My search"
        :items="menu"
        :session="session"
      >
        <MolgenisSession v-model="session" :key="timestamp" />
      </MolgenisMenu>
      <Breadcrumb
        v-if="showCrumbs && Object.keys(crumbs).length > 1"
        :crumbs="crumbs"
        :dropdown="schemaUrlsForCrumbs"
      />
      <div class="container-fluid p-3" style="padding-bottom: 50px">
        <h1 v-if="title">{{ title }}</h1>
        <slot />
      </div>
    </div>
    <MolgenisFooter>
      <span v-if="session && session.manifest">
        Software version:
        <a
          :href="
            'https://github.com/molgenis/molgenis-emx2/releases/tag/v' +
            session.manifest.SpecificationVersion
          "
        >
          {{ session.manifest.SpecificationVersion }} </a
        >.
        <span v-if="session.manifest.DatabaseVersion">
          Database version: {{ session.manifest.DatabaseVersion }}.
        </span>
      </span>
    </MolgenisFooter>
  </div>
</template>

<script>
import MolgenisMenu from "./MolgenisMenu.vue";
import MolgenisSession from "./MolgenisSession.vue";
import MolgenisFooter from "./MolgenisFooter.vue";
import Breadcrumb from "./Breadcrumb.vue";

/**
 Provides wrapper for your apps, including a little bit of contextual state, most notably 'account' that can be reacted to using v-model.
 */
export default {
  components: {
    MolgenisSession,
    MolgenisMenu,
    MolgenisFooter,
    Breadcrumb,
  },
  props: {
    menuItems: {
      type: Array,
      default: [
        { label: "Tables", href: "tables", role: "Viewer" },
        {
          label: "Schema",
          href: "schema",
          role: "Manager",
        },
        {
          label: "Up/Download",
          href: "updownload",
          role: "Editor",
        },
        {
          label: "Graphql",
          href: "graphql-playground",
          role: "Viewer",
        },
        {
          label: "Settings",
          href: "settings",
          role: "Manager",
        },
        {
          label: "Help",
          href: "docs",
          role: "Viewer",
        },
      ],
    },
    title: String,
    showCrumbs: {
      type: Boolean,
      default: true,
    },
  },
  data: function () {
    return {
      session: null,
      logoURL: null,
      fullscreen: false,
      timestamp: Date.now(),
    };
  },
  computed: {
    schemaUrlsForCrumbs() {
      var result = {};
      if (this.session && this.session.schemas) {
        //all databases
        result["list all databases"] = "/";
        this.session.schemas.forEach((s) => {
          result[s] = "../../" + s; // all paths are of form /:schema/:app
        });
      }
      return result;
    },
    crumbs() {
      if (window && location) {
        let path = decodeURI(
          window.location.pathname.replace(location.search, "")
        ).split("/");
        let url = "/";
        let result = {};
        if (window.location.pathname != "/apps/central/") {
          path.forEach((el) => {
            if (el != "") {
              url += el + "/";
              result[el] = url;
            }
          });
        }
        if (this.$route) {
          path = decodeURI(location.hash.split("?")[0]).substr(1).split("/");
          url += "#";
          path.forEach((el) => {
            if (el != "") {
              url += "/" + el;
              result[el] = url;
            }
          });
        }
        return result;
      }
      return {};
    },
    logoURLorDefault() {
      return (
        this.logoURL ||
        "/apps/molgenis-components/assets/img/molgenis_logo_white.png"
      );
    },
    menu() {
      if (this.session && this.session.settings && this.session.settings.menu) {
        return this.session.settings.menu;
      } else {
        return this.menuItems;
      }
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        if (this.session != undefined && this.session.settings) {
          if (this.session.settings.logoURL) {
            this.logoURL = this.session.settings.logoURL;
          }
        }
        this.$emit("input", this.session);
      },
    },
  },
  methods: {
    toggle() {
      this.fullscreen = !this.fullscreen;
    },
  },
};
</script>

<docs>
<template>
  <Molgenis :menuItems="[
        {label:'Home',href:'/'},
        {label:'My search',href:'http://google.com'},
        {label:'My movies',href:'http://youtube.com'}
     ]" title="My title" v-model="molgenis">
    <template>
      <p>Some contents and I can see the molgenis state via v-model = {{ JSON.stringify(molgenis) }}</p>
    </template>
  </Molgenis>
</template>
<script>
  export default {
    data() {
      return {
        molgenis: null
      }
    }
  }
</script>
</docs>
