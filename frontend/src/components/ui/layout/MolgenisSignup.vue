<template>
  <Spinner v-if="loading" />
  <div v-else-if="success">
    <MessageSuccess>{{ success }}</MessageSuccess>
    <ButtonAlt @click="cancel">
      Close
    </ButtonAlt>
  </div>
  <LayoutModal v-else :show="true" title="Sign up">
    <template #body>
      <LayoutForm>
        <MessageError v-if="error">
          {{ error }}
        </MessageError>
        <InputString
          v-model="email"
          help="Please enter your email address"
          label="Email address"
          placeholder="Enter valid email address"
        />
        <InputPassword
          v-model="password"
          help="Please enter the password"
          label="Password"
          placeholder="Enter password"
        />
        <InputPassword
          v-model="password2"
          help="Please enter the password again"
          label="Password Repeat"
          placeholder="Enter password"
          @keyup.enter="signup"
        />
      </LayoutForm>
    </template>
    <template #footer>
      <ButtonAlt @click="cancel">
        Cancel
      </ButtonAlt>
      <ButtonAction @click="signup">
        Sign up
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import ButtonAction from '../forms/ButtonAction.vue'
import ButtonAlt from '../forms/ButtonAlt.vue'
import InputPassword from '../forms/InputPassword.vue'
import InputString from '../forms/InputString.vue'
import LayoutForm from './LayoutForm.vue'
import LayoutModal from './LayoutModal.vue'
import MessageError from '../forms/MessageError.vue'
import MessageSuccess from '../forms/MessageSuccess.vue'
import {request} from 'graphql-request'
import Spinner from './Spinner.vue'

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputPassword,
    InputString,
    LayoutForm,
    LayoutModal,
    MessageError,
    MessageSuccess,
    Spinner,
  },
  emits: ['cancel'],
  data: function() {
    return {
      email: null,
      error: null,
      loading: false,
      password: null,
      password2: null,
      success: null,
    }
  },
  methods: {
    cancel() {
      /**
       * when cancel is pushed
       */
      this.error = null
      this.$emit('cancel')
    },
    signup() {
      if (
        this.email == null ||
        this.password == null ||
        this.password2 == null
      ) {
        this.error =
          'Error: valid email address and password should be filled in'
      } else if (this.password !== this.password2) {
        this.error = 'Error: Passwords entered must be the same'
      } else {
        this.error = null
        this.loading = true
        request(
          'graphql',
          `mutation{signup(email: "${this.email}", password: "${this.password}"){status,message}}`,
        )
          .then((data) => {
            if (data.signup.status === 'SUCCESS') {
              this.success = 'Success. Signed up with email: ' + this.email
            } else {
              this.error = 'Signup failed: ' + data.signup.message
            }
          })
          .catch((error) => {
            this.error = 'Sign up failed: ' + error.response.message
          })
        this.loading = false
      }
    },
  },
}
</script>
