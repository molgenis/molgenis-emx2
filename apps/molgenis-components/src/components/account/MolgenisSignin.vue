<template>
  <div v-if="success">
    <MessageSuccess>{{ success }}</MessageSuccess>
    <ButtonAlt @click="onCancel">Close</ButtonAlt>
  </div>
  <LayoutModal v-else title="Sign in" :show="true" @close="onCancel">
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
          autofocus />
        <InputPassword
          id="signInFormPassword"
          name="password"
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the provided password"
          @enterPressed="signin" />
        <div
          v-if="isPrivacyPolicyEnabled"
          class="alert"
          :class="error === privacyError ? 'alert-danger' : 'alert-info'"
          role="alert">
          <b>Privacy policy</b>
          <p>
            {{ privacyPolicy }}
          </p>
          <InputCheckbox
            class="mb-0"
            id="privacy-agreement"
            name="privacy-agreement"
            :required="true"
            :options="[privacyPolicyLabel]"
            :hideClearButton="true"
            v-model="userAgrees" />
        </div>
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="onCancel">Cancel</ButtonAlt>
      <ButtonSubmit form="signin-form"> Sign in </ButtonSubmit>
    </template>
  </LayoutModal>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { request } from "../../client/client";
import { privacyConstants } from "../constants.js";
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonSubmit from "../forms/ButtonSubmit.vue";
import LayoutForm from "../forms/FormMolgenis.vue";
import InputCheckbox from "../forms/InputCheckbox.vue";
import InputPassword from "../forms/InputPassword.vue";
import InputString from "../forms/InputString.vue";
import MessageError from "../forms/MessageError.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import { IResponse } from "./Interfaces";

const { POLICY_TEXT_KEY } = privacyConstants;

export default defineComponent({
  name: "MolgenisSignin",
  components: {
    ButtonAlt,
    InputPassword,
    InputString,
    MessageError,
    MessageSuccess,
    LayoutForm,
    LayoutModal,
    ButtonSubmit,
    InputCheckbox,
  },
  data: function () {
    return {
      email: null as string | null,
      password: null as string | null,
      error: null as string | null,
      success: null as string | null,
      loading: false,
      userAgrees: [],
      privacyPolicyLabel: "Agree with privacy policy",
      privacyPolicy: "" as string | undefined,
      isPrivacyPolicyEnabled: false,
      privacyError: "Please agree with the privacy policy",
    };
  },
  methods: {
    async signin() {
      if (!this.email || !this.password) {
        this.error = "Email and password should be filled in";
      } else if (
        this.isPrivacyPolicyEnabled &&
        this.userAgrees[0] !== this.privacyPolicyLabel
      ) {
        this.error = this.privacyError;
      } else {
        this.error = null;
        this.loading = true;
        request(
          "/api/graphql",
          `mutation{signin(email: "${this.email}", password: "${this.password}"){status,message}}`
        )
          .then(
            (data: {
              signin: {
                status: string;
                message: string;
              };
            }) => {
              if (data.signin.status === "SUCCESS") {
                this.success = "Signed in with " + this.email;
                this.$emit("signin", this.email);
              } else this.error = data.signin.message;
            }
          )
          .catch(
            error => (this.error = "internal server graphqlError" + error)
          );
        this.loading = false;
      }
    },
    onSignInFailed(msg: string) {
      this.error = msg;
      this.$emit("signInFailed", this.email);
    },
    onCancel() {
      this.error = null;
      this.$emit("cancel");
    },
    async fetchPrivacyPolicy() {
      const response: IResponse = await request(
        "graphql",
        `{_settings{key, value}}`
      );

      const policyData = response._settings.find(
        item => item.key === POLICY_TEXT_KEY
      );
      this.privacyPolicy = policyData?.value;

      const policyEnabledSettings = response._settings.find(item => {
        return item.key === "isPrivacyPolicyEnabled";
      });
      this.isPrivacyPolicyEnabled = policyEnabledSettings?.value === "true";
    },
  },
  watch: {
    async show(newValue) {
      if (newValue === true) await this.$nextTick();
      // set focus on email input to enable submit action
      (this.$refs as any).email.$el.children[1].focus();
    },
  },
  emits: ["cancel", "signInFailed", "signin"],
  mounted() {
    this.fetchPrivacyPolicy();
  },
});
</script>

<docs>
<template>
  <demo-item>
    <MolgenisSignin
        v-if="isShown"
        @cancel="isShown = false"
        @requestSignIn="handleSignInRequest(...arguments)"
    />
    <button type="button" class="btn" @click="isShown = true">Show</button>
  </demo-item>
</template>
<script>
  export default {
    data: function () {
      return {
        isShown: false,
      };
    },
    methods: {
      handleSignInRequest({email, password}) {
        alert(`handleSignInRequest, email = ${email}, pw = ${password}`);
      },
    },
  };
</script>
</docs>
