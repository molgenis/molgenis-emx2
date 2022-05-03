<template>
  <LayoutModal v-else title="Sign in" :show="true" @close="cancel">
    <template v-slot:body>
      <LayoutForm>
        <MessageError v-if="error">{{ error }}</MessageError>
        <InputString
          id="signin-email"
          v-model="email"
          label="Username"
          placeholder="Enter username"
          description="Please enter username"
        />
        <InputPassword
          id="signin-password"
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the provided password"
          @enterPressed="signin"
        />
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="cancel">Cancel</ButtonAlt>
      <ButtonAction @click="signin">Sign in</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import InputString from "../forms/InputString.vue";
import InputPassword from "../forms/InputPassword.vue";
import MessageError from "../forms/MessageError.vue";
import LayoutForm from "../forms/FormMolgenis.vue";
import LayoutModal from "./LayoutModal.vue";
import Spinner from "./Spinner.vue";

import { request } from "graphql-request";

export default {
  name: "MolgenisSignInForm",
  components: {
    ButtonAction,
    ButtonAlt,
    InputPassword,
    InputString,
    MessageError,
    LayoutForm,
    LayoutModal,
    Spinner,
  },
  data: function () {
    return {
      email: null,
      password: null,
      loading: false,
      error: null,
      success: null,
    };
  },
  methods: {
    signin() {
      if (this.email == null || this.password == null) {
        this.error = "Email and password should be filled in";
      } else {
        this.error = null;
        this.loading = true;
        request(
          "/api/graphql",
          `mutation{signin(email: "${this.email}", password: "${this.password}"){status,message}}`
        )
          .then((data) => {
            if (data.signin.status === "SUCCESS") {
              this.success = "Signed in with " + this.email;
              this.$emit("signin", this.email);
              location.reload();
            } else this.error = data.signin.message;
          })
          .catch(
            (error) => (this.error = "internal server graphqlError" + error)
          );
        this.loading = false;
      }
    },
    cancel() {
      /**
       * when cancel is pushed
       */
      this.error = null;
      this.$emit("cancel");
    },
  },
};
</script>

<docs>
<template>
  <div>
    <ButtonAction v-if="display == false" @click="display=true">Show</ButtonAction>
    <!-- normally you don't need graphqlURL because that is available in apps context-->
    <MolgenisSignIn v-else @login="signinTest" @cancel="display = false"/>
  </div>
</template>
<script>
  import {ButtonAction} from "molgenis-components";

  export default {
    components: {ButtonAction},
    data: function () {
      return {
        display: false,
        email: null
      };
    },
    methods: {
      signinTest(email, password) {
        alert("login with email " + email + " and password " + password);
        this.email = email;
      }
    }
  };
</script>
</docs>
