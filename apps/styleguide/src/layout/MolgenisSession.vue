<template>
  <Spinner v-if="loading" />
  <div v-else>
    <div>
      <span v-if="session.email && session.email != 'anonymous'">
        <a
          href="#"
          @click.prevent="showChangePasswordForm = true"
          class="text-light"
        >
          Hi {{ session.email }}</a
        >&nbsp;
        <ChangePasswordForm
          v-if="showChangePasswordForm"
          :error="error"
          @cancel="showChangePasswordForm = false"
        />
        <ButtonOutline @click="signout" :light="true">Sign out</ButtonOutline>
      </span>
      <span v-else>
        <ButtonOutline v-if="isOidcEnabled" href="/_login" :light="true">
          Sign in</ButtonOutline
        >
        <ButtonOutline v-else @click="showSigninForm = true" :light="true">
          Sign in</ButtonOutline
        >
        <SigninForm
          v-if="showSigninForm"
          @signin="changed"
          @cancel="closeSigninForm"
        />
        <ButtonAlt
          v-show="!isOidcEnabled"
          @click="showSignupForm = true"
          :light="true"
          >Sign up</ButtonAlt
        >
        <SignupForm
          v-if="showSignupForm"
          :error="error"
          @close="closeSignupForm"
        />
      </span>
    </div>
  </div>
</template>

<script>
import Spinner from "./Spinner";
import ButtonOutline from "../forms/ButtonOutline";
import ButtonAlt from "../forms/ButtonAlt";

import SigninForm from "./MolgenisSignin.vue";
import SignupForm from "./MolgenisSignup.vue";
import ChangePasswordForm from "./MolgenisAccount";

import { request } from "graphql-request";

const query = `{
  _session { email, roles, schemas },
  _settings (keys: ["menu", "page.", "cssURL", "logoURL", "isOidcEnabled"]){ key, value },
  _manifest { ImplementationVersion,SpecificationVersion,DatabaseVersion }
}`;

/** Element that is supposed to be put in menu holding all controls for user account */
export default {
  components: {
    ButtonOutline,
    SigninForm,
    SignupForm,
    ChangePasswordForm,
    Spinner,
    ButtonAlt,
  },
  props: {
    graphql: {
      default: "graphql",
      type: String,
    },
  },
  data: function () {
    return {
      /** @ignore */
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
      return (
        this.session &&
        this.session.settings &&
        this.session.settings["isOidcEnabled"] === "true"
      );
    },
  },
  methods: {
    async reload() {
      this.loading = true;

      const responses = await Promise.allSettled([
        request("/api/graphql", query),
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
        dbSettings._settings.forEach(
          (s) =>
            (this.session.settings[s.key] =
              s.value.startsWith("[") || s.value.startsWith("{")
                ? this.parseJson(s.value)
                : s.value)
        );
        this.session.manifest = dbSettings._manifest;
      }
      // schemaSettings override dbSettings if set
      if (schemaSettings && schemaSettings._settings) {
        schemaSettings._settings.forEach(
          (s) =>
            (this.session.settings[s.key] =
              s.value.startsWith("[") || s.value.startsWith("{")
                ? this.parseJson(s.value)
                : s.value)
        );
        this.session.manifest = schemaSettings._manifest;
      }

      this.loading = false;
      this.$emit("input", this.session);
    },
    handleError(reason) {
      this.error = "internal server error " + reason;
    },
    parseJson(value) {
      try {
        return JSON.parse(value);
      } catch (e) {
        this.error = "Parsing of settings failed: " + e + ". value: " + value;
        return null;
      }
    },
    changed() {
      this.reload();
      this.showSigninForm = false;
      this.$emit("input", this.session);
    },
    closeSigninForm() {
      this.showSigninForm = false;
      this.error = null;
    },
    closeSignupForm() {
      this.showSignupForm = false;
      this.error = null;
    },
    signout() {
      this.loading = true;
      this.showSigninForm = false;
      request("graphql", `mutation{signout{status}}`)
        .then((data) => {
          if (data.signout.status === "SUCCESS") {
            this.session = {};
          } else {
            this.error = "sign out failed";
          }
          this.loading = false;
          this.$emit("input", this.session);
          this.reload();
        })
        .catch((error) => (this.error = "internal server error" + error));
    },
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <MolgenisSession v-model="session" graphql="/pet store/graphql"/>
    <ShowMore title="debug">session = {{ session }}</ShowMore>
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
```
</docs>
