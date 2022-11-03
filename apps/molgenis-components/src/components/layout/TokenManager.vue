<template>
  <div>
    <div v-if="accessTokens.length">
    <label><b>Access tokens</b></label>
    <ul >
      <li v-for="(tokenName, idx) in accessTokens" :key="tokenName">{{ tokenName }}
        <IconDanger @click="deleteToken(idx)" icon="times"/>
      </li>
    </ul>
    </div>
    <MessageSuccess v-if="successMessage">{{ successMessage }}.
      <div v-if="lastTokenValue"><label><b>New token. Please copy for use</b></label>
        <pre>{{ lastTokenValue }}</pre>
      </div>
    </MessageSuccess>
    <MessageError v-if="errorMessage">{{ errorMessage }}</MessageError>
    <label><b>Create a token</b></label>
    <form class="form-inline">
    <InputString id="token-name" placeholder="new token name"
                 v-model="tokenName"/>
    <ButtonAction v-if="tokenName" :key="tokenName" @click="createToken">Create token
    </ButtonAction>
    </form>
  </div>
</template>
<script>
import ButtonAction from '../forms/ButtonAction.vue';
import IconDanger from '../forms/IconDanger.vue';
import InputString from '../forms/InputString.vue';
import MessageSuccess from '../forms/MessageSuccess.vue';
import MessageError from '../forms/MessageError.vue';
import {request} from '../../client/client.js'

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
export default {
  components: {
    ButtonAction,
    InputString,
    IconDanger,
    MessageSuccess,
    MessageError,
  },
  data() {
    return {
      session: null,
      tokenName: null,
      lastTokenValue: null,
      errorMessage: null,
      successMessage: null,
    };
  },
  computed: {
    accessTokens() {
      if (this.session && this.session.settings) {
        let result = this.session.settings.filter(setting => setting && setting.key === 'access-tokens' && setting.value).
            map(setting => setting.value.split(',').filter(value => value !== ""))
        if(result.length == 1) {
          return [
            ...new Set(result[0])];
        }
      }
      return [];
    },
  },
  methods: {
    async fetchSession() {
      const resp = await request('/api/graphql', query);
      this.session = resp._session;
    },
    clean() {
      this.errorMessage = null;
      this.successMessage = null;
      this.lastTokenValue = null;
    },
    async deleteToken(idx) {
      console.log(idx)
      this.clean();

      let newTokens = this.accessTokens;
      newTokens.splice(idx, 1);
      console.log(newTokens)

      const variables = {
        users:
          {
            email: this.session.email,
            settings: {
              key: 'access-tokens',
              value: newTokens.join(','),
            },
          },
      };

      request('/api/graphql', changeMutation, variables).then(result => {
        this.successMessage = "Token removed"
        this.lastTokenValue = null;
        this.fetchSession();
      }).catch((e) => {
        this.errorMessage = e.message;
      });
    },
    async createToken() {
      this.clean();
      const variables = {
        email: this.session.email,
        tokenName: this.tokenName,
      };
      request('/api/graphql', createMutation, variables).then(result => {
        this.successMessage = result.createToken.message;
        this.lastTokenValue = result.createToken.token;
        this.tokenName = null;
        this.fetchSession();
      }).catch((e) => {
        this.errorMessage = e.message;
      });
    },
  },
  mounted() {
    this.fetchSession();
  },
};
</script>
