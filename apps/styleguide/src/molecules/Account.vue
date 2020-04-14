<template>
  <Spinner v-if="loading" />
  <div v-else>
    <div>
      <span v-if="email">
        Hi {{ email }}
        <ButtonAction @click="signout">Sign out</ButtonAction>
      </span>
      <span v-else>
        <ButtonAction @click="showSigninForm = true">Sign in</ButtonAction>
        <SigninForm
          v-if="showSigninForm"
          :error="error"
          @signin="changed"
          @cancel="closeSigninForm"
        />
        <ButtonAlt @click="showSignupForm = true">Sign up</ButtonAlt>
        <SignupForm
          v-if="showSignupForm"
          :error="error"
          @cancel="closeSignupForm"
        />
      </span>
    </div>
  </div>
</template>

<script>
import Spinner from '../components/Spinner'
import ButtonAction from '../components/ButtonAction'
import ButtonAlt from '../components/ButtonAlt'

import SigninForm from './SigninForm.vue'
import SignupForm from './SignupForm.vue'

import { request } from 'graphql-request'

const endpoint = '/api/graphql'
/** Element that is supposed to be put in menu holding all controls for user account */
export default {
  components: {
    ButtonAction,
    SigninForm,
    SignupForm,
    Spinner,
    ButtonAlt
  },
  data: function() {
    return {
      /** @ignore */
      showSigninForm: false,
      showSignupForm: false,
      error: null,
      loading: false,
      email: null
    }
  },
  watch: {
    email() {
      this.showSigninForm = false
      this.showSignupForm = false
    }
  },
  created: function() {
    request(endpoint, `{_user{email}}`)
      .then(data => {
        if (data._user.email !== 'anonymous') {
          this.email = data._user.email
        } else {
          this.email = null
        }
      })
      .catch(error => {
        if (error.response.status === 504) {
          this.error = 'Error. Server cannot be reached.'
        } else {
          this.error = 'internal server error ' + error
        }
      })
  },
  methods: {
    changed(email) {
      this.email = email
      this.$emit('changed', this.email)
    },
    closeSigninForm() {
      this.showSigninForm = false
      this.error = null
    },
    closeSignupForm() {
      this.showSignupForm = false
      this.error = null
    },
    signout() {
      this.loading = true
      request(endpoint, `mutation{signout{status}}`)
        .then(data => {
          if (data.signout.status === 'SUCCESS') {
            this.email = null
            this.$emit('changed', this.email)
          } else this.error = 'sign out failed'
        })
        .catch(error => (this.error = 'internal server error' + error))
      this.loading = false
    }
  }
}
</script>

<docs>
    Example
    ```
    <Account/>
    ```
</docs>
