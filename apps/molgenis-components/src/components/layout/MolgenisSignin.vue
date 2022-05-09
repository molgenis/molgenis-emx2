<template>
  <div v-if="success">
    <MessageSuccess>{{ success }}</MessageSuccess>
    <ButtonAlt @click="onCancel">Close</ButtonAlt>
  </div>
  <LayoutModal v-else title="Sign in" :show="true" @close="onCancel">
    <template v-slot:body>
      <LayoutForm id="signin-form" @submit="signin">
        <MessageError v-if="error">{{ error }}</MessageError>
        <InputString
          id="signInFormEmail"
          ref="email"
          name="email"
          v-model="email"
          label="Username"
          placeholder="Enter username"
          description="Please enter username"
          autofocus
        />
        <InputPassword
          id="signInFormPassword"
          name="password"
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the provided password"
          @enterPressed="signin"
        />
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="onCancel">Cancel</ButtonAlt>
      <ButtonSubmit form="signin-form">Sign in</ButtonSubmit>
    </template>
  </LayoutModal>
</template>

<script>
import ButtonAlt from "../forms/ButtonAlt.vue";
import InputString from "../forms/InputString.vue";
import InputPassword from "../forms/InputPassword.vue";
import MessageError from "../forms/MessageError.vue";
import LayoutForm from "../forms/FormMolgenis.vue";
import LayoutModal from "./LayoutModal.vue";
import ButtonSubmit from "../forms/ButtonSubmit.vue";

import { request } from "../../client/client.js";

export default {
  name: "MolgenisSignin",
  components: {
    ButtonAlt,
    InputPassword,
    InputString,
    MessageError,
    LayoutForm,
    LayoutModal,
    ButtonSubmit,
  },
  data: function () {
    return {
      email: null,
      password: null,
      error: null,
      success: null,
    };
  },
  methods: {
    async signin() {
      if (!this.email || !this.password) {
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
    onSignInFailed(msg) {
      this.error = msg;
      this.$emit("signInFailed", this.email);
    },
    onCancel() {
      /**
       * when cancel is pushed
       */
      this.error = null;
      this.$emit("cancel");
    },
  },
  watch: {
    async show(newValue) {
      if (newValue === true) await this.$nextTick();
      // set focus on email input to enable submit action
      this.$refs.email.$el.children[1].focus();
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <MolgenisSignin
        v-if="isShown"
        @onCancel="isShown = false"
        @requestSignIn="handleSignInRequest(...arguments)"
    />
    <button type="button" class="btn" @click="isShown = true">Show</button>
  </demo-item>
</template>
<script>
  export default {
    data: function () {
      return {
        isShown: false,
      };
    },
    methods: {
      handleSignInRequest({email, password}) {
        alert(`handleSignInRequest, email = ${email}, pw = ${password}`);
      },
    },
  };
</script>
</docs>
