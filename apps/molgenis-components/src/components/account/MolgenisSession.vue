<template>
  <Spinner v-if="loading" />
  <div v-else>
    <div>
      <span v-if="session.email && session.email != 'anonymous'">
        <ButtonAlt @click="showChangePasswordForm = true" class="text-light">
          Hi {{ session.email }}</ButtonAlt
        >&nbsp;
        <MolgenisAccount
          v-if="showChangePasswordForm"
          :error="error"
          @cancel="showChangePasswordForm = false"
        />
        <ButtonOutline @click="signout" :light="true">Sign out</ButtonOutline>
      </span>
      <span v-else>
        <ButtonAlt
          v-show="!isOidcEnabled"
          @click="showSignupForm = true"
          :light="true"
        >
          Sign up
        </ButtonAlt>
        <SignupForm
          v-if="showSignupForm"
          :error="error"
          @close="closeSignupForm"
        />
        <ButtonOutline v-if="isOidcEnabled" href="/_login" :light="true">
          Sign in</ButtonOutline
        >
        <ButtonOutline v-else @click="showSigninForm = true" :light="true">
          Sign in</ButtonOutline
        >
        <MolgenisSignin
          v-if="showSigninForm"
          @signin="changed"
          @cancel="closeSigninForm"
        />
      </span>
      <LocaleSwitch
        v-if="locales.length > 1"
        class="ml-2"
        v-model="session.locale"
        :locales="locales"
      />
    </div>
  </div>
</template>

<script>
import Spinner from "../layout/Spinner.vue";
import ButtonOutline from "../forms/ButtonOutline.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";

import MolgenisSignin from "./MolgenisSignin.vue";
import SignupForm from "./MolgenisSignup.vue";
import MolgenisAccount from "./MolgenisAccount.vue";
import LocaleSwitch from "./LocaleSwitch.vue";

import { request } from "../../client/client.js";

const query = `{
  _session { email, roles, schemas, token, settings{key,value} },
  _settings (keys: ["menu", "page.", "cssURL", "logoURL", "isOidcEnabled","locales"]){ key, value },
  _manifest { ImplementationVersion,SpecificationVersion,DatabaseVersion }
}`;

/** Element that is supposed to be put in menu holding all controls for user account */
export default {
  components: {
    ButtonOutline,
    MolgenisSignin,
    SignupForm,
    MolgenisAccount,
    Spinner,
    ButtonAlt,
    LocaleSwitch,
  },
  props: {
    graphql: {
      default: "graphql",
      type: String,
    },
  },
  data: function () {
    return {
      showSigninForm: false,
      showSignupForm: false,
      showChangePasswordForm: false,
      error: null,
      loading: false,
      session: {},
      version: null,
    };
  },
  watch: {
    email() {
      this.showSigninForm = false;
      this.showSignupForm = false;
    },
  },
  created() {
    this.reload();
  },
  computed: {
    isOidcEnabled() {
      return this.session?.settings?.isOidcEnabled === "true";
    },
    locales() {
      if (this.session?.settings?.locales) {
        return this.session.settings.locales;
      } else {
        return ["en"];
      }
    },
  },
  methods: {
    loadSettings(settings) {
      console.log(settings);
      settings._settings.forEach(
        (s) =>
          (this.session.settings[s.key] =
            s.value?.startsWith("[") || s.value?.startsWith("{")
              ? this.parseJson(s.value)
              : s.value)
      );
    },
    async reload() {
      this.loading = true;

      const responses = await Promise.allSettled([
        request("/apps/central/graphql", query),
        request(this.graphql, query),
      ]);
      const dbSettings =
        responses[0].status === "fulfilled"
          ? responses[0].value
          : this.handleError(responses[0].reason);
      const schemaSettings =
        responses[1].status === "fulfilled"
          ? responses[1].value
          : this.handleError(responses[1].reason);

      if (schemaSettings && schemaSettings._session) {
        this.session = schemaSettings._session;
      } else {
        this.session = {};
      }
      //convert settings to object
      this.session.settings = {};
      if (dbSettings && dbSettings._settings) {
        this.loadSettings(dbSettings);
        this.session.manifest = dbSettings._manifest;
      }
      // schemaSettings override dbSettings if set
      if (schemaSettings && schemaSettings._settings) {
        this.loadSettings(schemaSettings);
        this.session.manifest = schemaSettings._manifest;
      }
      //set default locale
      if (this.session.locale === undefined) {
        this.session.locale = "en";
      }

      this.loading = false;
      this.$emit("update:modelValue", this.session);
    },
    handleError(reason) {
      this.error = "internal server error " + reason;
      if (
        reason?.response?.data?.errors &&
        reason.response.data.errors[0] &&
        reason.response.data.errors[0].message
      ) {
        this.$emit("error", reason.response.data.errors[0].message);
      } else {
        this.$emit("error", this.error);
      }
    },
    parseJson(value) {
      try {
        return JSON.parse(value);
      } catch (e) {
        this.error = "Parsing of settings failed: " + e + ". value: " + value;
        console.log(this.error);
        return null;
      }
    },
    changed() {
      this.reload();
      this.showSigninForm = false;
      this.$emit("update:modelValue", this.session);
    },
    closeSigninForm() {
      this.showSigninForm = false;
      this.error = null;
    },
    closeSignupForm() {
      this.showSignupForm = false;
      this.error = null;
    },
    async signout() {
      this.loading = true;
      this.showSigninForm = false;
      const data = await request("graphql", `mutation{signout{status}}`).catch(
        (error) => (this.error = "internal server error" + error)
      );
      if (data.signout.status === "SUCCESS") {
        this.session = {};
      } else {
        this.error = "sign out failed";
      }
      this.loading = false;
      this.$emit("update:modelValue", this.session);
      this.reload();
    },
  },
  emits: ["update:modelValue", "error"],
};
</script>

<docs>
<template>
  <div>
    <MolgenisSession class="bg-primary" v-model="session" graphql="/pet store/graphql"/>
    <pre>session = {{ session }}</pre>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        session: null
      }
    }
  }
</script>
</docs>
