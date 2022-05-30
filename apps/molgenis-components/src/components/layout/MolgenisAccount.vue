<template>
  <Spinner v-if="loading" />
  <LayoutModal v-else title="Change account" :show="true" @close="close">
    <template v-slot:body>
      <h2>Account information</h2>
      <table class="table table-bordered table-responsive">
        <tbody>
          <tr>
            <td>Username:</td>
            <td>{{ user }}</td>
          </tr>
          <tr>
            <td>Token:</td>
            <td>{{ token }}</td>
          </tr>
        </tbody>
      </table>
      <h2 class="mt-4">Change password</h2>
      <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
      <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
      <LayoutForm>
        <InputPassword
          id="account-password"
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the password"
          @enterPressed="updatePassword"
        />
        <InputPassword
          id="account-password2"
          v-model="password2"
          label="Password Repeat"
          placeholder="Enter password"
          description="Please enter the password again"
          @enterPressed="updatePassword"
        />
        <ButtonAction @click="updatePassword">Update password</ButtonAction>
      </LayoutForm>
      <h2 class="mt-4">Generate long-lived token</h2>
      <MessageSuccess v-if="tokenSuccess">{{ tokenSuccess }}</MessageSuccess>
      <MessageError v-if="tokenError">{{ tokenError }}</MessageError>
      <InputString
        id="tokenId"
        v-model="tokenName"
        label="TokenId"
        placeholder="Please add unique token id"
        description="Long lived token should have unique identifier "
        @enterPressed="createToken"
      />
      <ButtonAction @click="createToken">Create token</ButtonAction>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="close">Close</ButtonAlt>
    </template>
  </LayoutModal>
</template>

<script>
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import MessageError from "../forms/MessageError.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import Spinner from "./Spinner.vue";
import LayoutModal from "./LayoutModal.vue";
import InputPassword from "../forms/InputPassword.vue";

import { request } from "../../client/client.js";

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
    token: String,
  },
  data() {
    return {
      password: null,
      password2: null,
      tokenName: null,
      loading: false,
      graphqlError: null,
      success: null,
      tokenSuccess: null,
      tokenError: null,
    };
  },
  methods: {
    async createToken() {
      const data = await request(
        "graphql",
        `mutation{createToken(email: "${this.user}", tokenName: "${this.tokenName}"){status,message,token}}`
      ).catch((error) => {
        this.tokenError = JSON.stringify(error);
      });
      if (data.createToken.status === "SUCCESS") {
        this.tokenSuccess =
          "Success. Token created. Please copy for use: " +
          data.createToken.token;
      } else {
        this.tokenError =
          "Token generation failed: " + data.createToken.message;
      }
      this.loading = false;
    },
    async updatePassword() {
      if (this.password !== this.password2) {
        this.error = "Error: Passwords entered must be the same";
      } else {
        this.error = null;
        this.loading = true;
        const data = await request(
          "graphql",
          `mutation{changePassword(password: "${this.password}"){status,message}}`
        ).catch((error) => {
          this.graphqlError = JSON.stringify(error);
        });

        if (data.changePassword.status === "SUCCESS") {
          this.success = "Success. Password changed";
        } else {
          this.error = "Password change failed: " + data.changePassword.message;
        }
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

<docs>
<template>
  <div>
    <ButtonAlt @click="show = !show">toggle modal account view</ButtonAlt>
    <MolgenisAccount v-if="show" @cancel="show = false"/>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        show: false
      }
    }
  }
</script>
</docs>
