<template>
  <Spinner v-if="loading" />
  <LayoutModal v-else title="Change account" :show="true" @close="close">
    <template v-slot:body>
      <LayoutForm>
        <h2>Change password</h2>
        <MessageSuccess v-if="success">{{ success }}</MessageSuccess>
        <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
        <InputPassword
          v-model="password"
          label="Password"
          placeholder="Enter password"
          description="Please enter the password"
        />
        <InputPassword
          v-model="password2"
          label="Password Repeat"
          placeholder="Enter password"
          description="Please enter the password again"
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
import LayoutModal from "./LayoutModal";
import ButtonAction from "../forms/ButtonAction";
import ButtonAlt from "../forms/ButtonAlt";
import InputPassword from "../forms/InputPassword";
import MessageSuccess from "../forms/MessageSuccess";
import MessageError from "../forms/MessageError";
import Spinner from "./Spinner";
import { request } from "graphql-request";

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
    updatePassword() {
      if (this.password !== this.password2) {
        this.error = "Error: Passwords entered must be the same";
      } else {
        this.error = null;
        this.loading = true;
        request(
          "graphql",
          `mutation{changePassword(password: "${this.password}"){status,message}}`
        )
          .then((data) => {
            if (data.changePassword.status === "SUCCESS") {
              this.success = "Success. Password changed";
            } else {
              this.error =
                "Password change failed: " + data.changePassword.message;
            }
          })
          .catch((error) => {
            this.graphqlError = JSON.stringify(error);
          });
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
Example
```
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
```
</docs>
