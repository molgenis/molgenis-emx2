<template>
  <Spinner v-if="loading" />
  <LayoutModal v-else title="Change account" :show="true">
    <template v-slot:body>
      <LayoutForm>
        <h2>Change password</h2>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
        <MessageError v-if="error">{{ error }}</MessageError>
        <InputPassword
          v-model="password"
          label="Password"
          placeholder="Enter password"
          help="Please enter the password"
        />
        <InputPassword
          v-model="password2"
          label="Password Repeat"
          placeholder="Enter password"
          help="Please enter the password again"
        />
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="close">Close</ButtonAlt>
      <ButtonAction @click="updatePassword">Update password</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import LayoutModal from "./LayoutModal";
import ButtonAction from "../forms/ButtonAction";
import ButtonAlt from "../forms/ButtonAlt";
import InputPassword from "../forms/InputPassword";
import MessageSuccess from "../forms/MessageSuccess";
import MessageError from "../forms/MessageError";
import Spinner from "./Spinner";
import { request } from "graphql-request";

export default {
  components: {
    LayoutModal,
    ButtonAlt,
    ButtonAction,
    InputPassword,
    MessageError,
    MessageSuccess,
    Spinner,
  },
  props: {
    user: String,
  },
  data() {
    return {
      password: null,
      password2: null,
      loading: false,
      error: null,
      success: null,
    };
  },
  methods: {
    updatePassword() {
      console.log("start");
      if (this.password !== this.password2) {
        this.error = "Error: Passwords entered must be the same";
      } else {
        this.error = null;
        this.loading = true;
        request(
          "graphql",
          `mutation{changePassword(password: "${this.password}"){status,message}}`
        )
          .then((data) => {
            if (data.changePassword.status === "SUCCESS") {
              this.success = "Success. Password changed";
            } else {
              this.error =
                "Password change failed: " + data.changePassword.message;
            }
          })
          .catch((error) => {
            console.log(JSON.stringify(error));
          });
        this.loading = false;
      }
    },
    close() {
      this.error = null;
      this.$emit("cancel");
    },
  },
};
</script>
