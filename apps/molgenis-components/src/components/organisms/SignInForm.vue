<template>
  <LayoutModal title="Sign in" :show="true" @close="cancel">
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
          @enterPressed="signin"
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
import ButtonAction from '../forms/ButtonAction';
import ButtonAlt from '../forms/ButtonAlt';
import InputString from '../forms/InputString';
import InputPassword from '../forms/InputPassword';
import MessageError from '../forms/MessageError';
import LayoutForm from './LayoutForm';
import LayoutModal from './LayoutModal';

export default {
  name: 'SignInForm',
  components: {
    ButtonAction,
    ButtonAlt,
    InputPassword,
    InputString,
    MessageError,
    LayoutForm,
    LayoutModal
  },
  props: {
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
      if (this.email == null || this.password == null) {
        this.error = 'Email and password should be filled in';
      } else {
        this.error = null;

        if (!this.axiosClient) {
          this.$emit('requestSignIn', {email: this.email, password: this.password});
        }

        const signInResp = await this.axiosClient
          .post(
            '/api/graphql',
            `mutation{signin(email: "${this.email}", password: "${this.password}"){status,message}}`
          )
          .catch(
            (error) => (this.error = 'internal server graphqlError' + error)
          );

        if (signInResp.data.signin.status === 'SUCCESS') {
          this.success = 'Signed in with ' + this.email;
          this.$emit('signInSuccess', this.email);
          location.reload();
        } else {
          this.error = signInResp.data.signin.message;
          this.$emit('signInFailed', this.email);
        }
      }
    },
    cancel() {
      this.error = null;
      this.$emit('cancel');
    }
  }
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
