<template>
  <div>
    <div v-if="accessTokens.length">
      <label><b>Access tokens</b></label>
      <ul>
        <li v-for="(tokenName, idx) in accessTokens" :key="tokenName">
          {{ tokenName }}
          <IconDanger @click="deleteToken(idx)" icon="times" />
        </li>
      </ul>
    </div>
    <MessageSuccess v-if="successMessage"
      >{{ successMessage }}.
      <div v-if="lastTokenValue">
        <label><b>New token. Please copy for use</b></label>
        <pre>{{ lastTokenValue }}</pre>
      </div>
    </MessageSuccess>
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
    <label><b>Create a token</b></label>
    <form class="form-inline">
      <InputString
        id="token-name"
        placeholder="new token name"
        v-model="tokenName" />
      <ButtonAction v-if="tokenName" :key="tokenName" @click="createToken"
        >Create token
      </ButtonAction>
    </form>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { request } from "../../client/client.js";
import { IError } from "../../Interfaces/IError";
import { ISetting } from "../../Interfaces/ISetting";
import ButtonAction from "../forms/ButtonAction.vue";
import IconDanger from "../forms/IconDanger.vue";
import InputString from "../forms/InputString.vue";
import MessageError from "../forms/MessageError.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import { ISession, IResponse } from "./Interfaces";

const query = `{_session { email, token, settings{key,value}}}`;
const changeMutation = `mutation change($users:[UsersInput]){
        change(users:$users){
            message
          }
        }`;
const createMutation = `mutation createToken($email:String,$tokenName:String){
        createToken(email:$email,tokenName:$tokenName){
            message,token
          }
        }`;
export default defineComponent({
  components: {
    ButtonAction,
    InputString,
    IconDanger,
    MessageSuccess,
    MessageError,
  },
  data() {
    return {
      session: null as null | ISession,
      tokenName: null as null | string,
      lastTokenValue: null as null | string,
      errorMessage: null as null | string,
      successMessage: null as null | string,
    };
  },
  computed: {
    accessTokens(): string[] {
      const tokens: ISetting | undefined = this.session?.settings?.find(
        (setting: ISetting): boolean =>
          setting.key === "access-tokes" && Boolean(setting.value)
      );
      return tokens
        ? tokens.value
            .split(",")
            .filter((value: string): boolean => value !== "")
        : [];
    },
  },
  methods: {
    async fetchSession() {
      const resp: IResponse = await request("/api/graphql", query);
      this.session = resp._session;
    },
    clean() {
      this.errorMessage = null;
      this.successMessage = null;
      this.lastTokenValue = null;
    },
    async deleteToken(idx: number) {
      this.clean();

      let newTokens: string[] = this.accessTokens;
      newTokens.splice(idx, 1);

      const variables = {
        users: {
          email: this.session?.email,
          settings: {
            key: "access-tokens",
            value: newTokens.join(","),
          },
        },
      };

      request("/api/graphql", changeMutation, variables)
        .then(() => {
          this.successMessage = "Token removed";
          this.lastTokenValue = null;
          this.fetchSession();
        })
        .catch((error: IError) => {
          this.errorMessage = error.message;
        });
    },
    async createToken() {
      this.clean();
      const variables = {
        email: this.session?.email,
        tokenName: this.tokenName,
      };
      request("/api/graphql", createMutation, variables)
        .then((result: { createToken: { message: string; token: string } }) => {
          this.successMessage = result.createToken.message;
          this.lastTokenValue = result.createToken.token;
          this.tokenName = null;
          this.fetchSession();
        })
        .catch((error: IError) => {
          this.errorMessage = error.message;
        });
    },
  },
  mounted() {
    this.fetchSession();
  },
});
</script>
