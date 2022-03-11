<template>
  <Spinner v-if="loading" />
  <div v-else-if="success">
    <MessageSuccess>{{ success }}</MessageSuccess>
    <ButtonAlt @click="cancel">Close</ButtonAlt>
  </div>
  <LayoutModal v-else title="Sign in" :show="true" @close="cancel">
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
import {
  ButtonAction,
  ButtonAlt,
  InputString,
  InputPassword,
  MessageError,
  MessageSuccess,
  LayoutForm,
  LayoutModal,
  Spinner
} from 'molgenis-components';

export default {
  components: {
    ButtonAction,
    ButtonAlt,
    InputPassword,
    InputString,
    MessageError,
    MessageSuccess,
    LayoutForm,
    LayoutModal,
    Spinner
  },
  data: function () {
    return {
      email: null,
      password: null,
      loading: false,
      error: null,
      success: null
    };
  },
  methods: {
    signin() {
      // handle signin success or failure 
    },
    cancel() {
      /**
       * when cancel is pushed
       */
      this.error = null;
      this.$emit('cancel');
    }
  }
};
</script>

