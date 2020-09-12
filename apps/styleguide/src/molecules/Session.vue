<template>
  <Spinner v-if="loading" />
  <div v-else>
    <div>
      <span v-if="session.email">
        Hi {{ session.email }}
        <ButtonAction @click="signout">Sign out</ButtonAction>
      </span>
      <span v-else>
        <ButtonAction @click="showSigninForm = true">Sign in</ButtonAction>
        <SigninForm
          v-if="showSigninForm"
          :error="error"
          @signin="changed"
          @cancel="closeSigninForm"
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
import Spinner from "../components/Spinner";
import ButtonAction from "../components/ButtonAction";
import ButtonAlt from "../components/ButtonAlt";

import SigninForm from "./SigninForm.vue";
import SignupForm from "./SignupForm.vue";

import { request } from "graphql-request";

/** Element that is supposed to be put in menu holding all controls for user account */
export default {
  components: {
    ButtonAction,
    SigninForm,
    SignupForm,
    Spinner,
    ButtonAlt
  },
  data: function() {
    return {
      /** @ignore */
      showSigninForm: false,
      showSignupForm: false,
      error: null,
      loading: false,
      session: null,
      version: null
    };
  },
  watch: {
    email() {
      this.showSigninForm = false;
      this.showSignupForm = false;
    }
  },
  created() {
    this.reload();
  },
  methods: {
    reload() {
      this.loading = true;
      request("graphql", `{_session{email,roles}}`)
        .then(data => {
          if (data._session.email !== "anonymous") {
            this.session = data._session;
          } else {
            this.session = {};
          }

          this.loading = false;
          this.$emit("input", this.session);
        })
        .catch(error => {
          if (error.response.status === 504) {
            this.error = "Error. Server cannot be reached.";
          } else {
            this.error = "internal server error " + error;
          }
          this.loading = false;
        });
    },
    changed() {
      this.reload();
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
      request("graphql", `mutation{signout{status}}`)
        .then(data => {
          if (data.signout.status === "SUCCESS") {
            this.session = {};
          } else {
            this.error = "sign out failed";
          }
          this.loading = false;
          this.$emit("input", this.session);
        })
        .catch(error => (this.error = "internal server error" + error));
    }
  }
};
</script>

<docs>
    Example
    ```
    <template>
        <div>
            <Session v-model="session"/>
            <ShowMore title="debug">sesionn = {{session}}</ShowMore>
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
