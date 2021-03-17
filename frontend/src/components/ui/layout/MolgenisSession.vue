<template>
    <Spinner v-if="loading" />
    <div v-else>
        <div>
            <MessageError v-if="error">
                {{ error }}
            </MessageError>
            <span v-if="session.email && session.email != 'anonymous'">
                <a href="#" @click.prevent="showChangePasswordForm = true">
                    Hi {{ session.email }}</a>&nbsp;
                <ChangePasswordForm
                    v-if="showChangePasswordForm"
                    :error="error"
                    @cancel="showChangePasswordForm = false"
                />
                <ButtonAction @click="signout">Sign out</ButtonAction>
            </span>
            <span v-else>
                <ButtonAction @click="showSigninForm = true">Sign in</ButtonAction>
                <SigninForm
                    v-if="showSigninForm"
                    :error="error"
                    @cancel="closeSigninForm"
                    @signin="changed"
                />
                <ButtonAlt @click="showSignupForm = true">Sign up</ButtonAlt>
                <SignupForm
                    v-if="showSignupForm"
                    :error="error"
                    @cancel="closeSignupForm"
                />
            </span>
        </div>
    </div>
</template>

<script>
import Spinner from "./Spinner.vue";
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import MessageError from "../forms/MessageError.vue";

import SigninForm from "./MolgenisSignin.vue";
import SignupForm from "./MolgenisSignup.vue";
import ChangePasswordForm from "./MolgenisAccount.vue";

import { request } from "graphql-request";

/** Element that is supposed to be put in menu holding all controls for user account */
export default {
  components: {
    ButtonAction,
    SigninForm,
    SignupForm,
    ChangePasswordForm,
    Spinner,
    ButtonAlt,
    MessageError,
  },
  props: {
    graphql: {
      default: "graphql",
      type: String,
    },
  },
  emits: ['update:modelValue'],
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
  methods: {
    reload() {
      this.loading = true;
      request(
        this.graphql,
        `{_session{email,roles},_settings{key,value},_manifest{ImplementationVersion,SpecificationVersion}}`
      )
        .then((data) => {
          if (data._session != undefined) {
            this.session = data._session;
          } else {
            this.session = {};
          }
          //convert settings to object
          this.session.settings = {};
          data._settings.forEach(
            (s) =>
              (this.session.settings[s.key] =
                s.value.startsWith("[") || s.value.startsWith("{")
                  ? this.parseJson(s.value)
                  : s.value)
          );
          this.session.manifest = data._manifest;
          this.loading = false;
          this.$emit('update:modelValue', this.session);
        })
        .catch((error) => {
          if (error.response.status === 504) {
            this.error = "Error. Server cannot be reached.";
          } else {
            this.error = "internal server error " + error;
          }
          this.loading = false;
        });
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
      this.$emit('update:modelValue', this.session);
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
          this.$emit('update:modelValue', this.session);
          this.reload();
        })
        .catch((error) => (this.error = "internal server error" + error));
    },
  },
};
</script>
