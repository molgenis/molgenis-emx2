<template>
  <div>
    <Spinner v-if="loading" />
    <div class="container">
      <h1>Account settings</h1>
      <ul class="nav nav-tabs">
        <li class="nav-item">
          <a
            class="nav-link"
            :class="{ active: view == 'passwordView' }"
            href="#"
            @click="view = 'passwordView'"
            >Change password</a
          >
        </li>
        <li class="nav-item">
          <a
            class="nav-link"
            href="#"
            :class="{ active: view == 'tokenView' }"
            @click="view = 'tokenView'"
            >Get tokens</a
          >
        </li>
        <li class="nav-item">
          <a
            class="nav-link"
            href="#"
            :class="{ active: view == 'settingsView' }"
            @click="view = 'settingsView'"
            >Settings</a
          >
        </li>
      </ul>
      <div class="bg-white container">
        <div v-if="view == 'passwordView'">
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
        </div>
        <div v-if="view == 'tokenView'">
          <h2>Security tokens</h2>
          <p>Here you can retrieve JWT tokens used for programmatic access.</p>
          <h3>Short term token.</h3>
          <p>You can use this token for 60 minutes.</p>
          <pre>{{ token }}</pre>
          <h3 class="mt-4">Generate long-lived token</h3>
          <p>You can generate a token that is valid until you revoke it.</p>
          <MessageSuccess v-if="tokenSuccess"
            >{{ tokenSuccess }}
          </MessageSuccess>
          <MessageError v-if="tokenError">{{ tokenError }}</MessageError>
          <InputString
            class="w-75"
            id="tokenId"
            v-model="tokenName"
            placeholder="Please add unique token id"
            @enterPressed="createToken"
          />
          <ButtonAction @click="createToken">Create token</ButtonAction>
          <h3 class="mt-4">Revoke tokens</h3>
          {{ settings }}
        </div>
      </div>
    </div>
  </div>
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
    settings: Array,
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
      view: "passwordView",
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
