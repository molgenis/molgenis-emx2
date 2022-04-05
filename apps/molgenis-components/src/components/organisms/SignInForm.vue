<template>
  <LayoutModal title="Sign in" :show="show" @close="cancel">
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
      <ButtonAlt @click="cancel">Cancel</ButtonAlt>
      <ButtonSubmit form="signin-form">Sign in</ButtonSubmit>
    </template>
  </LayoutModal>
</template>

<script>
import ButtonAlt from '../forms/ButtonAlt.vue';
import InputString from '../forms/InputString.vue';
import InputPassword from '../forms/InputPassword.vue';
import MessageError from '../forms/MessageError.vue';
import LayoutForm from '../forms/FormMolgenis.vue';
import LayoutModal from '../layout/LayoutModal.vue';
import ButtonSubmit from '../forms/ButtonSubmit.vue';

export default {
  name: 'SignInForm',
  components: {
    ButtonAlt,
    InputPassword,
    InputString,
    MessageError,
    LayoutForm,
    LayoutModal,
    ButtonSubmit
  },
  props: {
    show: {
      type: Boolean,
      required: false,
      default: () => false
    },
    axiosClient: {
      type: Object,
      required: false
    }
  },
  data: function () {
    return {
      email: null,
      password: null,
      error: null,
      success: null
    };
  },
  methods: {
    async signin() {
      if (!this.email || !this.password) {
        this.error = 'Email and password should be filled in';
      } else {
        this.error = null;

        if (!this.axiosClient) {
          this.$emit('requestSignIn', {
            email: this.email,
            password: this.password,
            onSignSuccess: this.onSignSuccess,
            onSignInFailed: this.onSignInFailed
          });
          return;
        }

        const signInResp = await this.axiosClient
          .post(
            '/api/graphql',
            {query: `mutation{signin(email: "${this.email}", password: "${this.password}"){status,message}}`}
          )
          .catch(
            (error) => (this.error = 'internal server graphqlError' + error)
          );

        if (signInResp.data.data.signin.status === 'SUCCESS') {
          this.onSignSuccess();
        } else {
          this.onSignInFailed(signInResp.data.data.signin.message);
        }
      }
    },
    onSignSuccess() {
      this.success = 'Signed in with ' + this.email;
      this.$emit('signInSuccess', this.email);
      if (location) {
        location.reload();
      }
    },
    onSignInFailed(msg) {
      this.error = msg;
      this.$emit('signInFailed', this.email);
    },
    cancel() {
      this.error = null;
      this.$emit('cancel');
    }
  },
  watch: {
    async show(newValue) {
      if (newValue === true)
        await this.$nextTick()
        // set focus on email input to enable submit action
        this.$refs.email.$el.children[1].focus()
    }
  }
};
</script>
