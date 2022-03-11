<template>
  <div class="wrapper d-flex flex-column">
    <Menu :brandHref="brandHref" :menu="menu">
      <ButtonOutline v-if="isOidcEnabled" href="/_login" :light="true">
        Sign in</ButtonOutline
      >
      <ButtonOutline v-else @click="showSigninForm" :light="true">
        Sign in</ButtonOutline
      >
    </Menu>
    <Breadcrumb v-if="isBreadcrumbShown" :crumbs="crumbs" />
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

    <LayoutModal title="Sign in"></LayoutModal>
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
import {Breadcrumb, ButtonOutline, LayoutModal} from 'molgenis-components';
import {mapGetters} from 'vuex';
export default {
  components: {Breadcrumb, ButtonOutline, LayoutModal},
  computed: {
    ...mapGetters(['menu', 'isOidcEnabled']),
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
      return '/' + this.schema;
    },
    crumbs() {
      const sections = this.$route.path
        .split('/')
        .filter((section) => section !== '');

      // given a path section walk the path (building the url) until section is found
      const buildUrl = (section) => {
        return sections.reduce((url, current) => {
          return url.split('/').pop() !== section ? url + '/' + current : url;
        });
      };

      return sections.reduce((accum, section) => {
        // add "/" to make absolute path
        const routeUrl = '/' + buildUrl(section);
        accum[section] = routeUrl;
        return accum;
      }, {});
    },
    isBreadcrumbShown() {
      return this.$route.path !== '/apps/central/';
    }
  },
  methods: {
    showSigninForm () {
      console.log('show signin from')
    }
  }
};
</script>