<template>
  <Spinner v-if="loading" />
  <LayoutModal v-else-if="success" title="Sign up" :show="true">
    <template v-slot:body>
      <MessageSuccess>{{ success }}</MessageSuccess>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="close">Close</ButtonAlt>
    </template>
  </LayoutModal>
  <LayoutModal v-else title="Sign up" :show="true" @close="close">
    <template v-slot:body>
      <LayoutForm>
        <MessageError v-if="error">{{ error }}</MessageError>
        <InputString
          id="signup-email"
          v-model="email"
          label="Email address"
          placeholder="Enter valid email address"
          description="Please enter your email address"
        />
        <InputPassword
          id="signup-password"
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the password"
          @enterPressed="signup"
        />
        <InputPassword
          id="signup-password-repeat"
          v-model="passwordRepeat"
          label="Password Repeat"
          placeholder="Enter password"
          description="Please enter the password again"
          @enterPressed="signup"
        />
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="close">Cancel</ButtonAlt>
      <ButtonAction @click="signup">Sign up</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import Spinner from "./Spinner.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import MessageError from "../forms/MessageError.vue";
import LayoutModal from "./LayoutModal.vue";
import InputPassword from "../forms/InputPassword.vue";
import InputString from "../forms/InputString.vue";
import LayoutForm from "./LayoutForm.vue";

import { request } from "../../client/client.js";

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputPassword,
    InputString,
    MessageError,
    MessageSuccess,
    LayoutForm,
    Spinner,
    LayoutModal,
  },
  data: function () {
    return {
      email: null,
      password: null,
      passwordRepeat: null,
      loading: false,
      error: null,
      success: null,
    };
  },
  methods: {
    signup() {
      if (
        this.email == null ||
        this.password == null ||
        this.passwordRepeat == null
      ) {
        this.error =
          "Error: valid email address and password should be filled in";
      } else if (this.password !== this.passwordRepeat) {
        this.error = "Error: Passwords entered must be the same";
      } else {
        this.error = null;
        this.loading = true;
        request(
          "/api/graphql",
          `mutation{signup(email: "${this.email}", password: "${this.password}"){status,message}}`
        )
          .then((data) => {
            if (data.signup.status === "SUCCESS") {
              this.success = "Success. Signed up with email: " + this.email;
            } else {
              this.error = "Signup failed: " + data.signup.message;
            }
          })
          .catch((error) => {
            this.error = "Sign up failed: " + error.response.message;
          });
        this.loading = false;
      }
    },
    close() {
      /**
       * when close is pushed
       */
      this.error = null;
      this.$emit("close");
    },
  },
};
</script>

<docs>
<template>
  <div>
    <ButtonAction v-if="display == false" @click="display=true">Show</ButtonAction>
    <MolgenisSignup v-else @signup="SignupTest" @close="display = false"/>
  </div>
</template>
<script>
  export default {
    data: function () {
      return {
        display: false,
        email: null
      };
    },
    methods: {
      SignupTest(email, password) {
        alert("sign up with email " + email + " and password " + password);
        this.email = email;
      }
    }
  };
</script>
</docs>
