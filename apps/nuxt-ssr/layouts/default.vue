<template>
  <div class="wrapper d-flex flex-column">
    <Menu :brandHref="brandHref" :menu="menu" />
    <BreadCrumb v-if="isBreadCrumbShown" :crumbs="crumbs" />
    <Nuxt class="flex-fill" />
    <molgenis-footer class="footer">
      <span v-if="session && manifest">
        Software version:
        <a
          :href="
            'https://github.com/molgenis/molgenis-emx2/releases/tag/v' +
            manifest.SpecificationVersion
          "
          >{{ manifest.SpecificationVersion }}</a
        >.
        <span v-if="manifest.DatabaseVersion"
          >Database version: {{ manifest.DatabaseVersion }}.</span
        >
      </span>
    </molgenis-footer>
  </div>
</template>

<style>
body,
div.wrapper {
  min-height: 100vh;
}

.footer {
  margin-top: 6rem;
  margin-bottom: 2rem;
}
</style>

<script>
import { BreadCrumb } from "molgenis-components";
import { mapGetters } from "vuex";
export default {
  components: { BreadCrumb },
  computed: {
    ...mapGetters(["menu"]),
    schema() {
      return this.$store.state.schema;
    },
    session() {
      return this.$store.state.session;
    },
    manifest() {
      return this.$store.state.manifest;
    },
    brandHref() {
      return "/" + this.schema;
    },
    crumbs() {
      const sections = this.$route.path
        .split("/")
        .filter((section) => section !== "");

      // given a path section walk the path (building the url) until section is found
      const buildUrl = (section) => {
        return sections.reduce((url, current) => {
          return url.split("/").pop() !== section ? url + "/" + current : url;
        });
      };

      return sections.reduce((accum, section) => {
        // add "/" to make absolute path
        const routeUrl = "/" + buildUrl(section);
        accum[section] = routeUrl;
        return accum;
      }, {});
    },
    isBreadCrumbShown() {
      return this.$route.path !== "/apps/central/";
    },
  },
};
</script>