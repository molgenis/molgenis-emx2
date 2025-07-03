<template>
  <div style="background-color: #f4f4f4">
    <slot v-if="$slots.banner" name="banner" />
    <CookieWall
      v-if="analyticsId"
      :analyticsId="analyticsId"
      :htmlContentString="cookieWallContent"
    />
    <div style="min-height: calc(100vh - 70px)">
      <MolgenisMenu
        :logo="logoURLorDefault"
        active="My search"
        :items="menu"
        :session="session"
      >
        <MolgenisSession
          v-model="session"
          :key="timestamp"
          @error="$emit('error', $event)"
        />
      </MolgenisMenu>
      <Breadcrumb
        v-if="showCrumbs && Object.keys(crumbs).length > 1"
        :crumbs="crumbs"
        :dropdown="schemaUrlsForCrumbs"
      />
      <main class="container-fluid p-3" style="padding-bottom: 50px">
        <h1 v-if="title">{{ title }}</h1>
        <slot />
      </main>
    </div>
    <footer>
      <div
        v-if="session?.settings?.additionalFooterHtml"
        v-html="session?.settings?.additionalFooterHtml"
      ></div>
      <slot v-if="$slots.footer" name="footer" />
      <MolgenisFooter>
        <span v-if="session?.manifest">
          Software version:
          <a
            :href="
              'https://github.com/molgenis/molgenis-emx2/releases/tag/' +
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
    </footer>
  </div>
</template>

<script lang="ts">
import MolgenisMenu from "./MolgenisMenu.vue";
import MolgenisSession from "../account/MolgenisSession.vue";
import MolgenisFooter from "./MolgenisFooter.vue";
import Breadcrumb from "./Breadcrumb.vue";
import CookieWall from "./CookieWall.vue";
import { request, gql } from "graphql-request";

const defaultSchemaMenuItems = [
  { label: "Tables", href: "tables", role: "Viewer" },
  {
    label: "Schema",
    href: "schema",
    role: "Viewer",
  },
  {
    label: "Up/Download",
    href: "updownload",
    role: "Viewer",
  },
  {
    label: "Reports",
    href: "reports",
    role: "Viewer",
  },
  {
    label: "Jobs & Scripts",
    href: "tasks",
    role: "Manager",
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
];

/**
 Provides wrapper for your apps, including a little bit of contextual state, most notably 'account' that can be reacted to using v-model.
 */
export default {
  components: {
    MolgenisSession,
    MolgenisMenu,
    MolgenisFooter,
    Breadcrumb,
    CookieWall,
  },
  props: {
    menuItems: {
      type: Array,
    },
    title: String,
    showCrumbs: {
      type: Boolean,
      default: true,
    },
  },
  data: function () {
    return {
      session: {} as Record<string, any>,
      logoURL: null,
      fullscreen: false,
      timestamp: Date.now(),
      analyticsId: null,
      cookieWallContent: null,
    };
  },
  computed: {
    schemaUrlsForCrumbs() {
      let result: Record<string, any> = {
        "list all databases": "/apps/central/",
      };
      //all databases
      if (this.session?.schemas) {
        const appName = this.menu[0]?.href || "tables";
        this.session.schemas
          .sort((a: string, b: string) =>
            a.localeCompare(b, undefined, { sensitivity: "base" })
          )
          .forEach((schema: string) => {
            result[schema] = "../../" + schema + "/" + appName; // all paths are of form /:schema/:app/tables
          });
      }
      return result;
    },
    crumbs() {
      let result: Record<string, any> = {};
      if (window && location) {
        let path = decodeURI(
          window.location.pathname.replace(location.search, "")
        ).split("/");
        let url = "/";
        if (window.location.pathname != "/apps/central/") {
          path.forEach((el) => {
            if (el !== "") {
              url += el + "/";
              result[el] = url;
            }
          });
        }
        if (this.$route) {
          path = decodeURI(location.hash.split("?")[0]).substr(1).split("/");
          url += "#";
          path.forEach((el) => {
            if (el !== "") {
              url += "/" + el;
              result[el] = url;
            }
          });
        }
      }
      return result;
    },
    logoURLorDefault() {
      return (
        this.logoURL ||
        "/apps/molgenis-components/assets/img/molgenis_logo_white.png"
      );
    },
    menu() {
      if (this.menuItems) {
        return this.menuItems;
      } else if (this.session?.settings?.menu) {
        return this.session.settings.menu;
      } else {
        return defaultSchemaMenuItems;
      }
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        if (this.session?.settings?.logoURL) {
          this.logoURL = this.session.settings.logoURL;
        }
        const additionalJs: string = this.session?.settings?.additionalJs;
        if (additionalJs) {
          try {
            ("use strict");
            eval?.(`(function() {"use strict"; ${additionalJs}})()`);
          } catch (error) {
            console.log(error);
          }
        }
        this.$emit("update:modelValue", this.session);
      },
    },
  },
  methods: {
    toggle() {
      this.fullscreen = !this.fullscreen;
    },
  },
  emits: ["update:modelValue", "error"],
  created() {
    request(
      "graphql",
      gql`
        {
          _settings {
            key
            value
          }
        }
      `
    ).then((data: any) => {
      const analyticsSetting = data._settings.find(
        (setting: Record<string, any>) => setting.key === "ANALYTICS_ID"
      );
      this.analyticsId = analyticsSetting ? analyticsSetting.value : null;
      const analyticsCookieWallContentSetting = data._settings.find(
        (setting: Record<string, any>) =>
          setting.key === "ANALYTICS_COOKIE_WALL_CONTENT"
      );
      this.cookieWallContent = analyticsCookieWallContentSetting
        ? analyticsCookieWallContentSetting.value
        : null;
    });
  },
};
</script>

<docs>
<template>
  <DemoItem>
    <Molgenis
      :menuItems="[
        { label: 'Home', href: '/' },
        { label: 'My search', href: 'http://google.com' },
        { label: 'My movies', href: 'http://youtube.com' },
      ]"
      title="My title"
      v-model="molgenis"
    >
      <template>
        <p>
          Some contents and I can see the molgenis state via v-model =
          {{ JSON.stringify(molgenis) }}
        </p>
      </template>
    </Molgenis>
  </DemoItem>
  <DemoItem>
    <Molgenis
      :menuItems="[
        { label: 'Home', href: '/' },
        { label: 'My search', href: 'http://google.com' },
        { label: 'My movies', href: 'http://youtube.com' },
      ]"
      title="Footer title"
      v-model="molgenis"
    >
      <template>
        <p>WithCustom Footer</p>
      </template>
      <template #footer>A fully custom footer</template>
    </Molgenis>
  </DemoItem>

  <DemoItem>
    <Molgenis
      :menuItems="[
        { label: 'Home', href: '/' },
        { label: 'My search', href: 'http://google.com' },
        { label: 'My movies', href: 'http://youtube.com' },
      ]"
      title="With banner title"
      v-model="molgenis"
    >
      <template #banner>
        <div class="mg_banner"> 
          This is text in a banner
        </div>
      </template>
      <template #footer>footer</template>
    </Molgenis>
  </DemoItem>

</template>
<script>
export default {
  data() {
    return {
      molgenis: null,
    };
  },
};
</script>

<style>
.mg_banner {
  background-color: #72f6b2;
  padding: 10px;
  text-align: center;
}
</style>
</docs>
