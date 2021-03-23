<template>
  <Spinner v-if="loading" />
  <LayoutModal
    v-else :show="true"
    title="Change account"
    @close="close"
  >
    <template #body>
      <LayoutForm>
        <h2>Change password</h2>
        <MessageSuccess v-if="success">
          {{ success }}
        </MessageSuccess>
        <MessageError v-if="graphqlError">
          {{ graphqlError }}
        </MessageError>
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
        />
      </LayoutForm>
    </template>
    <template #footer>
      <ButtonAlt @click="close">
        Close
      </ButtonAlt>
      <ButtonAction @click="updatePassword">
        Update password
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import LayoutModal from './LayoutModal.vue'
import ButtonAction from '../forms/ButtonAction.vue'
import ButtonAlt from '../forms/ButtonAlt.vue'
import InputPassword from '../forms/InputPassword.vue'
import MessageSuccess from '../forms/MessageSuccess.vue'
import MessageError from '../forms/MessageError.vue'
import Spinner from './Spinner.vue'

import {request} from 'graphql-request'

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
      graphqlError: null,
      success: null,
    }
  },
  methods: {
    updatePassword() {
      console.log('start')
      if (this.password !== this.password2) {
        this.error = 'Error: Passwords entered must be the same'
      } else {
        this.error = null
        this.loading = true
        request(
          'graphql',
          `mutation{changePassword(password: "${this.password}"){status,message}}`,
        )
          .then((data) => {
            if (data.changePassword.status === 'SUCCESS') {
              this.success = 'Success. Password changed'
            } else {
              this.error =
                'Password change failed: ' + data.changePassword.message
            }
          })
          .catch((error) => {
            this.graphqlError = JSON.stringify(error)
          })
        this.loading = false
      }
    },
    close() {
      this.error = null
      this.$emit('cancel')
    },
  },
}
</script>
