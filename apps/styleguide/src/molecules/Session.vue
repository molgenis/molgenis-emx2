<template>
  <Spinner v-if="loading" />
  <div v-else>
    <div>
      <span v-if="error">
        <MessageError>{{ error }}</MessageError>
      </span>
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
import MessageError from "../components/MessageError";

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
    ButtonAlt,
    MessageError
  },
  props: {
    graphql: {
      default: "graphql",
      type: String
    }
  },
  data: function() {
    return {
      /** @ignore */
      showSigninForm: false,
      showSignupForm: false,
      error: null,
      loading: false,
      session: {},
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
      request(
        this.graphql,
        `{_session{email,roles},_settings{key,value},_manifest{ImplementationVersion,SpecificationVersion}}`
      )
        .then(data => {
          if (
            data._session != undefined &&
            data._session.email !== "anonymous"
          ) {
            this.session = data._session;
          } else {
            this.session = {};
          }
          //convert settings to object
          this.session.settings = {};
          data._settings.forEach(
            s =>
              (this.session.settings[s.key] =
                s.value.startsWith("[") || s.value.startsWith("{")
                  ? this.parseJson(s.value)
                  : s.value)
          );
          this.session.manifest = data._manifest;
          console.log(JSON.stringify(data));
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
        .then(data => {
          if (data.signout.status === "SUCCESS") {
            this.session = {};
            console.log("signed out");
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
    <Session v-model="session" graphql="/graphql/pet store"/>
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
