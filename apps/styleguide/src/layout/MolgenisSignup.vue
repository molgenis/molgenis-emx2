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
  <LayoutModal v-else title="Sign up" :show="true">
    <template v-slot:body>
      <LayoutForm>
        <MessageError v-if="error">{{ error }}</MessageError>
        <InputString
          v-model="email"
          label="Email address"
          placeholder="Enter valid email address"
          description="Please enter your email address"
        />
        <InputPassword
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the password"
        />
        <InputPassword
          v-model="password2"
          label="Password Repeat"
          placeholder="Enter password"
          description="Please enter the password again"
          @keyup.enter="signup"
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
import ButtonAction from "../forms/ButtonAction";
import ButtonAlt from "../forms/ButtonAlt";
import InputString from "../forms/InputString";
import InputPassword from "../forms/InputPassword";
import MessageError from "../forms/MessageError";
import MessageSuccess from "../forms/MessageSuccess";
import LayoutForm from "./LayoutForm";
import Spinner from "./Spinner";
import LayoutModal from "./LayoutModal";

import { request } from "graphql-request";

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
      password2: null,
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
        this.password2 == null
      ) {
        this.error =
          "Error: valid email address and password should be filled in";
      } else if (this.password !== this.password2) {
        this.error = "Error: Passwords entered must be the same";
      } else {
        this.error = null;
        this.loading = true;
        request(
          "graphql",
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
Example
```
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
```

</docs>
