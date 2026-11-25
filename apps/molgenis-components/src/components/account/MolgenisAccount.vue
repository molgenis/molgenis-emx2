<template>
  <LayoutModal title="Change account" :show="true" @close="close">
    <template v-slot:body>
      <FormMolgenis id="change-password-form" @submit="updatePassword">
        <h2>Change password</h2>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
        <MessageError v-if="error">{{ error }}</MessageError>
        <!-- lets password managers link the new password to the right account -->
        <input
          v-if="user"
          type="text"
          name="username"
          autocomplete="username"
          :value="user"
          readonly
          hidden
        />
        <InputPassword
          id="account-old-password"
          v-model="oldPassword"
          label="Current password"
          placeholder="Enter current password"
          description="Please enter your current password"
          autocomplete="current-password"
        />
        <InputPassword
          id="account-password"
          v-model="password"
          label="New password"
          placeholder="Enter new password"
          description="Please enter the new password"
          autocomplete="new-password"
        />
        <InputPassword
          id="account-password2"
          v-model="password2"
          label="Repeat new password"
          placeholder="Enter new password"
          description="Please enter the new password again"
          autocomplete="new-password"
        />
        <ButtonSubmit :disabled="loading">Update password</ButtonSubmit>
        <Spinner v-if="loading" />
      </FormMolgenis>
      <h2 class="mt-4">Manage tokens</h2>
      <TokenManager />
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="close">Close</ButtonAlt>
    </template>
  </LayoutModal>
</template>

<script lang="ts">
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonSubmit from "../forms/ButtonSubmit.vue";
import FormMolgenis from "../forms/FormMolgenis.vue";
import MessageError from "../forms/MessageError.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import Spinner from "../layout/Spinner.vue";
import LayoutModal from "../layout/LayoutModal.vue";
import InputPassword from "../forms/InputPassword.vue";
import TokenManager from "./TokenManager.vue";
import { defineComponent } from "vue";
import { request } from "../../client/client";

const CHANGE_PASSWORD_MUTATION = `mutation changePassword($oldPassword: String, $password: String) {
  changePassword(oldPassword: $oldPassword, password: $password) { status, message }
}`;

export default defineComponent({
  components: {
    LayoutModal,
    ButtonAlt,
    ButtonSubmit,
    FormMolgenis,
    InputPassword,
    MessageError,
    MessageSuccess,
    Spinner,
    TokenManager,
  },
  props: {
    user: String,
  },
  data() {
    return {
      oldPassword: null as null | string,
      password: null as null | string,
      password2: null as null | string,
      loading: false,
      success: null as null | string,
      error: null as null | string,
    };
  },
  methods: {
    async updatePassword() {
      if (this.loading) {
        return;
      }
      this.success = null;
      this.error = null;
      if (!this.oldPassword) {
        this.error = "Please enter your current password";
        return;
      }
      if (!this.password) {
        this.error = "Please enter a new password";
        return;
      }
      if (this.password !== this.password2) {
        this.error = "New passwords entered must be the same";
        return;
      }

      this.loading = true;
      try {
        // passwords are sent as variables so special characters cannot break the query
        const data = await request("graphql", CHANGE_PASSWORD_MUTATION, {
          oldPassword: this.oldPassword,
          password: this.password,
        });
        if (data?.changePassword?.status === "SUCCESS") {
          this.success = "Password changed";
          this.oldPassword = null;
          this.password = null;
          this.password2 = null;
        } else {
          this.error =
            data?.changePassword?.message ?? "Password change failed";
          this.oldPassword = null;
        }
      } catch (error) {
        this.error = "Password change failed: " + error;
        this.oldPassword = null;
      } finally {
        this.loading = false;
      }
    },
    close() {
      this.oldPassword = null;
      this.password = null;
      this.password2 = null;
      this.error = null;
      this.$emit("cancel");
    },
  },
  emits: ["cancel"],
});
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
