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
        :session="session ?? undefined"
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
      />
      <slot v-if="$slots.footer" name="footer" />
      <MolgenisFooter v-else>
        <span v-if="session?.manifest">
          Software version:
          <a
            :href="
              'https://github.com/molgenis/molgenis-emx2/releases/tag/' +
              session.manifest.SpecificationVersion
            "
          >
            {{ session.manifest.SpecificationVersion }}
          </a>
          .
          <span v-if="session.manifest.DatabaseVersion">
            Database version: {{ session.manifest.DatabaseVersion }}.
          </span>
        </span>
      </MolgenisFooter>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ISetting } from "metadata-utils";
import { computed, onMounted, ref, watch } from "vue";
import Client from "../../client/client";
import { MenuItem } from "../../Interfaces/MenuItem";
import MolgenisSession from "../account/MolgenisSession.vue";
import Breadcrumb from "./Breadcrumb.vue";
import CookieWall from "./CookieWall.vue";
import MolgenisFooter from "./MolgenisFooter.vue";
import MolgenisMenu from "./MolgenisMenu.vue";
import { getContextPath } from "../../utils/contextPath";

const defaultSchemaMenuItems: MenuItem[] = [
  {
    label: "Tables",
    href: "tables",
    role: "Viewer",
    submenu: [],
  },
  {
    label: "Schema",
    href: "schema",
    role: "Viewer",
    submenu: [],
  },
  {
    label: "Up/Download",
    href: "updownload",
    role: "Viewer",
    submenu: [],
  },
  {
    label: "Reports",
    href: "reports",
    role: "Viewer",
    submenu: [],
  },
  {
    label: "Jobs & Scripts",
    href: "tasks",
    role: "Manager",
    submenu: [],
  },
  {
    label: "Graphql",
    href: "graphql-playground",
    role: "Viewer",
    submenu: [],
  },
  {
    label: "Settings",
    href: "settings",
    role: "Manager",
    submenu: [],
  },
  {
    label: "Help",
    href: "docs",
    role: "Viewer",
    submenu: [],
  },
];

const props = withDefaults(
  defineProps<{
    title: string;
    menuItems?: MenuItem[];
    showCrumbs?: boolean;
  }>(),
  { menuItems: () => [], showCrumbs: true }
);

const session = ref<Record<string, any> | null>(null);
const logoURL = ref<string | null>(null);

const menu = ref<MenuItem[]>([]);

if (props.menuItems.length) {
  menu.value = toEmx2AppLocation(props.menuItems);
} else {
  menu.value = toEmx2AppLocation(defaultSchemaMenuItems);
}

const emit = defineEmits<{
  (e: "update:modelValue", value: Record<string, any> | null): void;
  (e: "error", error: any): void;
}>();
const timestamp = ref(Date.now());
const analyticsId = ref<string | null>(null);
const cookieWallContent = ref<string | null>(null);

const contextPath = computed(() => getContextPath());

const schemaUrlsForCrumbs = computed(() => {
  const cp = contextPath.value;
  let result: Record<string, any> = {
    "list all databases": cp + "/apps/central/",
  };
  if (session.value?.schemas) {
    session.value.schemas
      .sort((a: string, b: string) =>
        a.localeCompare(b, undefined, { sensitivity: "base" })
      )
      .forEach((schema: string) => {
        result[schema] = cp + "/" + schema + "/index";
      });
  }
  return result;
});

const crumbs = computed(() => {
  let result: Record<string, any> = {};
  if (window && location) {
    const cp = contextPath.value;
    const contextSegmentCount = cp.split("/").filter(Boolean).length;

    let path = decodeURI(
      window.location.pathname.replace(location.search, "")
    ).split("/");
    let url = cp + "/";
    if (!window.location.pathname.includes("/apps/central/")) {
      let appPath = "index";
      let segIdx = 0;
      path.forEach((el) => {
        if (el !== "") {
          segIdx++;
          if (segIdx <= contextSegmentCount) {
            return; // skip context path segments in breadcrumb
          }
          url += el + "/";
          result[el] = url + appPath;
          appPath = "";
        }
      });
    }

    path = decodeURI(location?.hash?.split("?")[0]).substr(1).split("/");
    url += "#";
    path.forEach((el) => {
      if (el !== "") {
        url += "/" + el;
        result[el] = url;
      }
    });
  }
  return result;
});

const logoURLorDefault = computed(() => {
  return (
    logoURL.value ??
    contextPath.value +
      "/apps/molgenis-components/assets/img/molgenis_logo_white.png"
  );
});

watch(
  session,
  (newValue) => {
    if (newValue?.settings?.logoURL) {
      logoURL.value = newValue.settings.logoURL;
    }
    const additionalJs: string = newValue?.settings?.additionalJs;
    if (additionalJs) {
      try {
        ("use strict");
        eval?.(`(function() {"use strict"; ${additionalJs}})()`);
      } catch (error) {
        console.log(error);
      }
    }

    if (!props.menuItems.length && newValue?.settings?.menu) {
      menu.value = newValue.settings.menu;
    }
    emit("update:modelValue", newValue);
  },
  { deep: true }
);

watch(
  () => props.menuItems,
  (newValue) => {
    if (newValue.length) {
      menu.value = toEmx2AppLocation(newValue);
    } else {
      menu.value = toEmx2AppLocation(defaultSchemaMenuItems);
    }
  }
);

// Re-apply when context path resolves (session schemas loaded)
watch(contextPath, () => {
  const items = props.menuItems.length ? props.menuItems : defaultSchemaMenuItems;
  menu.value = toEmx2AppLocation(items);
});

function toEmx2AppLocation(menuItems: MenuItem[]) {
  const cp = getContextPath();
  const cpSegmentCount = cp.split("/").filter(Boolean).length;
  const pathSegments =
    window?.location?.pathname.split("/")?.filter(Boolean) ?? [];

  if (!pathSegments.length) return menuItems;

  // Under /apps/: prefix absolute hrefs with context path
  if (pathSegments.includes("apps")) {
    if (!cp) return menuItems;
    return menuItems.map((menuItem: MenuItem) => ({
      ...menuItem,
      href: menuItem.href?.startsWith("/") ? cp + menuItem.href : menuItem.href,
      submenu: (menuItem.submenu ?? []).map((subItem) => ({
        ...subItem,
        href: subItem.href?.startsWith("/") ? cp + subItem.href : subItem.href,
      })),
    }));
  }

  // Under /:contextPath*/:schema/:app — skip context path segments to get schema name
  const schemaName = pathSegments[cpSegmentCount];
  if (!schemaName) return menuItems;

  function rewriteHref(href: string): string {
    let location = `${cp}/${schemaName}/${href}`;
    let hashLocation = location.indexOf("#");
    if (hashLocation !== -1) {
      const charBeforeHash = location.substring(hashLocation - 1, hashLocation);
      if (charBeforeHash !== "/") {
        location =
          location.substring(0, hashLocation) +
          "/" +
          location.substring(hashLocation, location?.length);
      }
    }
    hashLocation = location.indexOf("#");
    if (hashLocation !== -1) {
      return location;
    }
    if (!location.endsWith("/")) {
      return location + "/";
    }
    return location;
  }

  return menuItems.map((menuItem: MenuItem) => ({
    ...menuItem,
    href: rewriteHref(menuItem.href),
    submenu: (menuItem.submenu ?? []).map((subItem) => ({
      ...subItem,
      href: rewriteHref(subItem.href),
    })),
  }));
}

onMounted(async () => {
  const client = Client.newClient();
  const settings = await client.fetchSettings();

  const analyticsSetting = settings.find(
    (setting: ISetting) => setting.key === "ANALYTICS_ID"
  );

  analyticsId.value = analyticsSetting ? analyticsSetting.value : null;
  const analyticsCookieWallContentSetting = settings.find(
    (setting: ISetting) => setting.key === "ANALYTICS_COOKIE_WALL_CONTENT"
  );

  cookieWallContent.value = analyticsCookieWallContentSetting
    ? analyticsCookieWallContentSetting.value
    : null;
});
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
