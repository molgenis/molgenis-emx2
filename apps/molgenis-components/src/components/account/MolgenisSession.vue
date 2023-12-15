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
        <ButtonOutline
          @click="signout"
          :light="!lightMode"
          :class="`${lightMode ? 'btn-outline-dark' : ''}`"
          >Sign out</ButtonOutline
        >
      </span>
      <span v-else>
        <ButtonAlt
          v-show="!isOidcEnabled"
          @click="showSignupForm = true"
          :light="!lightMode"
          :class="`${lightMode ? 'btn-outline-dark' : ''}`"
        >
          Sign up
        </ButtonAlt>
        <SignupForm
          v-if="showSignupForm"
          :error="error"
          @close="closeSignupForm"
        />
        <ButtonOutline
          v-if="isOidcEnabled"
          :href="oidcLoginUrl"
          :light="!lightMode"
          :class="`${lightMode ? 'btn-outline-dark' : ''}`"
        >
          Sign in</ButtonOutline
        >
        <ButtonOutline
          v-else
          @click="showSigninForm = true"
          :light="!lightMode"
          :class="`${lightMode ? 'btn-outline-dark' : ''}`"
        >
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

<script lang="ts">
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonOutline from "../forms/ButtonOutline.vue";
import Spinner from "../layout/Spinner.vue";
import MolgenisSignin from "./MolgenisSignin.vue";
import SignupForm from "./MolgenisSignup.vue";
import MolgenisAccount from "./MolgenisAccount.vue";
import LocaleSwitch from "./LocaleSwitch.vue";
import { useCookies } from "vue3-cookies";
import { defineComponent } from "vue";
import { request } from "../../client/client.js";
import { IErrorMessage, IResponse, ISession } from "./Interfaces";
import { ISetting } from "meta-data-utils";

const { cookies } = useCookies();
const query = `{
  _session { email, roles, schemas, token, settings{key,value} },
  _settings (keys: ["menu", "page.", "cssURL", "logoURL", "isOidcEnabled","locales"]){ key, value },
  _manifest { ImplementationVersion,SpecificationVersion,DatabaseVersion }
}`;
const defaultSession = { locale: "en", settings: {} };

/** Element that is supposed to be put in menu holding all controls for user account */
export default defineComponent({
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
    lightMode: {
      type: Boolean,
      default: false,
    },
  },
  data: function () {
    return {
      showSigninForm: false,
      showSignupForm: false,
      showChangePasswordForm: false,
      error: null as string | null,
      loading: false,
      session: defaultSession as ISession,
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
    oidcLoginUrl() {
      const redirectParam = window?.location?.href
        ? `?redirect=${window.location.href}`
        : "";
      return "/_login" + redirectParam;
    },
    locales() {
      if (this.session?.settings?.locales) {
        if (Array.isArray(this.session.settings.locales)) {
          return this.session.settings.locales;
        } else {
          this.error =
            'locales should be array similar to ["en"] but instead was ' +
            JSON.stringify(this.session.settings.locales);
        }
      }
      //default
      return ["en"];
    },
  },
  methods: {
    loadSettings(settings: { _settings: ISetting[] }) {
      settings._settings.forEach((setting) => {
        const value: string =
          setting.value?.startsWith("[") || setting.value?.startsWith("{")
            ? this.parseJson(setting.value)
            : setting.value;
        this.session.settings[setting.key] = value;
      });
    },
    async reload() {
      this.loading = true;

      const responses: PromiseSettledResult<IResponse>[] =
        await Promise.allSettled([
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
        this.session = defaultSession;
      }
      //convert settings to object
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
        //get from cookie
        const lang = cookies.get("MOLGENIS.locale");
        if (lang) {
          this.session.locale = lang;
        }
      }
      //get the map
      this.loading = false;
      this.$emit("update:modelValue", this.session);
    },
    handleError(reason: IErrorMessage) {
      this.error = "internal server error " + reason;
      if (reason?.response?.data?.errors[0]?.message) {
        this.$emit("error", reason.response.data.errors[0].message);
      } else {
        this.$emit("error", this.error);
      }
    },
    parseJson(value: string) {
      try {
        return JSON.parse(value);
      } catch (error) {
        this.error =
          "Parsing of settings failed: " + error + ". value: " + value;
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
        (error: string) => (this.error = "internal server error" + error)
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
});
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
