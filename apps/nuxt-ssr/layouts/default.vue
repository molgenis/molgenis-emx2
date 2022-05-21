<template>
  <div class="wrapper d-flex flex-column">
    <Menu :logo="logo" :brandHref="brandHref" :menu="menu" :light="true">
      <template v-if="!isSignendIn">
        <ButtonOutline v-if="isOidcEnabled" href="/_login" :light="true">
          Sign in
        </ButtonOutline>
        <ButtonOutline v-else @click="showSignInForm = true" :light="true">
          Sign in
        </ButtonOutline>
        <MolgenisSignin
          v-if="showSignInForm"
          @cancel="showSignInForm = false"
          @signin="signIn(...arguments)"
        />
      </template>
      <template v-else>
        <span class="text-light mr-1">{{ email }}</span>
        <ButtonOutline
          v-if="isSignendIn"
          @click="signOut({ onSignOutFailed })"
          :light="true"
          >Sign out
        </ButtonOutline>
      </template>
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
import { Breadcrumb, ButtonOutline, MolgenisSignin, MolgenisFooter } from "molgenis-components";
import { mapGetters, mapActions } from "vuex";

export default {
  components: { Breadcrumb, ButtonOutline, MolgenisSignin, MolgenisFooter },
  data() {
    return {
      showSignInForm: false,
    };
  },
  computed: {
    ...mapGetters(["menu", "logo", "isOidcEnabled"]),
    isSignendIn() {
      return (
        this.session && this.session.email && this.session.email !== "anonymous"
      );
    },
    email() {
      return this.session ? this.session.email : null;
    },
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
    isBreadcrumbShown() {
      return this.$route.path !== "/apps/central/";
    },
  },
  methods: {
    ...mapActions(["signIn", "signOut"]),
    onSignOutFailed(msg) {
      console.log(msg);
    },
  },
};
</script>
