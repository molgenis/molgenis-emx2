<template>
  <Spinner v-if="loading" />
  <LayoutModal v-else title="Change account" :show="true" @close="close">
    <template v-slot:body>
      <LayoutForm>
        <h2>Change password</h2>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
        <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
        <InputPassword
          id="account-password"
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the password"
          @enterPressed="updatePassword"
        />
        <InputPassword
          id="account-password2"
          v-model="password2"
          label="Password Repeat"
          placeholder="Enter password"
          description="Please enter the password again"
          @enterPressed="updatePassword"
        />
      </LayoutForm>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="close">Close</ButtonAlt>
      <ButtonAction @click="updatePassword">Update password</ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import MessageError from "../forms/MessageError.vue";
import MessageSuccess from "../forms/MessageSuccess.vue";
import Spinner from "./Spinner.vue";
import LayoutModal from "./LayoutModal.vue";
import InputPassword from "../forms/InputPassword.vue";

import { request } from "../../client/client.js";

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
    };
  },
  methods: {
    async updatePassword() {
      if (this.password !== this.password2) {
        this.error = "Error: Passwords entered must be the same";
      } else {
        this.error = null;
        this.loading = true;
        const data = await request(
          "graphql",
          `mutation{changePassword(password: "${this.password}"){status,message}}`
        ).catch((error) => {
          this.graphqlError = JSON.stringify(error);
        });

        if (data.changePassword.status === "SUCCESS") {
          this.success = "Success. Password changed";
        } else {
          this.error = "Password change failed: " + data.changePassword.message;
        }
        this.loading = false;
      }
    },
    close() {
      this.error = null;
      this.$emit("cancel");
    },
  },
};
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
