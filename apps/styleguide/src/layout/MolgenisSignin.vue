<template>
  <Spinner v-if="loading" />
  <div v-else-if="success">
    <MessageSuccess>{{ success }}</MessageSuccess>
    <ButtonAlt @click="cancel">Close</ButtonAlt>
  </div>
  <LayoutModal v-else title="Sign in" :show="true" @close="cancel">
    <template v-slot:body>
      <LayoutForm>
        <MessageError v-if="error">{{ error }}</MessageError>
        <InputString
          v-model="email"
          label="Username"
          placeholder="Enter username"
          description="Please enter username"
        />
        <InputPassword
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the provided password"
          @keyup.enter="signin"
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
import ButtonAction from "../forms/ButtonAction";
import ButtonAlt from "../forms/ButtonAlt";
import InputString from "../forms/InputString";
import InputPassword from "../forms/InputPassword";
import MessageError from "../forms/MessageError";
import MessageSuccess from "../forms/MessageSuccess";
import LayoutForm from "./LayoutForm";
import LayoutModal from "./LayoutModal";
import Spinner from "./Spinner";
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
Example
```
<template>
  <div>
    <ButtonAction v-if="display == false" @click="display=true">Show</ButtonAction>
    <!-- normally you don't need graphqlURL because that is available in apps context-->
    <MolgenisSignin v-else @login="signinTest" @cancel="display = false"/>
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
      signinTest(email, password) {
        alert("login with email " + email + " and password " + password);
        this.email = email;
      }
    }
  };
</script>
```

</docs>
