<template>
  <div style="background-color: #f4f4f4">
    <div style="min-height: calc(100vh - 70px)">
      <MolgenisTheme
        href="https://fonts.googleapis.com/css?family=Oswald:500|Roboto|Roboto+Mono&display=swap"
      />
      <MolgenisMenu
        :logo="logo"
        active="My search"
        :items="menu"
        :session="session"
      >
        <MolgenisSession v-model="session" :key="timestamp" />
      </MolgenisMenu>
      <Breadcrumb
        v-if="showCrumbs && Object.keys(crumbs).length > 1"
        :crumbs="crumbs"
      />
      <div class="container-fluid p-3" style="padding-bottom: 50px">
        <h1 v-if="title">{{ title }}</h1>
        <slot />
      </div>
    </div>
    <Footer>
      <span v-if="session && session.manifest">
        Software version:
        <a
          :href="
            'https://github.com/molgenis/molgenis-emx2/releases/tag/v' +
            session.manifest.SpecificationVersion
          "
          >{{ session.manifest.SpecificationVersion }}</a
        >.
        <span v-if="session.manifest.DatabaseVersion"
          >Database version:
          <a
            :href="
              'https://github.com/molgenis/molgenis-emx2/releases/tag/v' +
              session.manifest.DatabaseVersion
            "
            >{{ session.manifest.DatabaseVersion }}</a
          >.</span
        >
      </span>
    </Footer>
  </div>
</template>

<script>
import MolgenisMenu from "./MolgenisMenu";
import MolgenisSession from "./MolgenisSession";
import MolgenisTheme from "./MolgenisTheme";
import Footer from "./MolgenisFooter";
import DefaultMenuMixin from "../mixins/DefaultMenuMixin";
import Breadcrumb from "./Breadcrumb";

/**
 Provides wrapper for your apps, including a little bit of contextual state, most notably 'account' that can be reacted to using v-model.
 */
export default {
  components: {
    MolgenisSession,
    MolgenisMenu,
    Footer,
    MolgenisTheme,
    Breadcrumb,
  },
  mixins: [DefaultMenuMixin],
  props: {
    menuItems: Array,
    title: String,
    showCrumbs: {
      type: Boolean,
      default: true,
    },
  },
  data: function () {
    return {
      session: null,
      cssURL: null,
      logoURL: null,
      cssLoaded: false,
      fullscreen: false,
      timestamp: Date.now(),
    };
  },
  computed: {
    crumbs() {
      this.$route;
      let path = decodeURI(
        window.location.pathname.replace(location.search, "")
      ).split("/");
      let url = "/";
      let result = { databases: url };
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
    },
    logo() {
      if (this.logoURL) return this.logoURL;
      else return "/apps/styleguide/assets/img/molgenis_logo_white.png";
    },
    css() {
      if (this.cssURL) return this.cssURL;
      else return "theme.css";
    },
    menu() {
      if (this.session && this.session.settings && this.session.settings.menu) {
        return this.session.settings.menu;
      } else if (this.menuItems) {
        return this.menuItems;
      } else {
        return this.defaultMenu;
      }
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        if (this.session != undefined && this.session.settings) {
          if (this.session.settings.cssURL) {
            this.cssURL = this.session.settings.cssURL;
          }
          if (this.session.settings.logoURL) {
            this.logoURL = this.session.settings.logoURL;
          }
        }
        //load themeCss
        fetch(this.css).then(() => {
          this.cssLoaded = true;
        });
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
```
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
```
</docs>
