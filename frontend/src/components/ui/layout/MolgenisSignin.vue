<template>
    <Spinner v-if="loading" />
    <div v-else-if="success">
        <MessageSuccess>{{ success }}</MessageSuccess>
        <ButtonAlt @click="cancel">
            Close
        </ButtonAlt>
    </div>
    <LayoutModal
        v-else :show="true"
        title="Sign in"
        @close="cancel"
    >
        <template #body>
            <LayoutForm>
                <MessageError v-if="error">
                    {{ error }}
                </MessageError>
                <InputString
                    v-model="email"
                    help="Please enter the provided email address"
                    label="Email"
                    placeholder="Enter email adress"
                />
                <InputPassword
                    v-model="password"
                    help="Please enter the provided password"
                    label="Password"
                    placeholder="Enter password"
                    @keyup.enter="signin"
                />
            </LayoutForm>
        </template>
        <template #footer>
            <ButtonAlt @click="cancel">
                Cancel
            </ButtonAlt>
            <ButtonAction @click="signin">
                Sign in
            </ButtonAction>
        </template>
    </LayoutModal>
</template>

<script>
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import InputString from "../forms/InputString.vue";
import InputPassword from "../forms/InputPassword.vue";
import MessageError from "../forms/MessageError.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import LayoutForm from "./LayoutForm.vue";
import LayoutModal from "./LayoutModal.vue";
import Spinner from "./Spinner.vue";

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
          "graphql",
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
