<template>
  <div>
    <h2>Create/alter user</h2>
    <MessageSuccess v-if="alterSuccess">{{ alterSuccess }}</MessageSuccess>
    <MessageError v-if="alterError">{{ alterError }}</MessageError>
    <Spinner v-if="alterLoading"></Spinner>
    <form v-else class="form-inline bg-white">
      <InputString
        label="username"
        v-model="username"
        placeholder="Enter username"
      />
      <InputPassword
        label="password"
        v-model="password"
        placeholder="Enter password"
      />
      <ButtonAction @click="alterUser" class="mt-0">Update user</ButtonAction>
    </form>
    <h2>User list</h2>
    <TableSimple class="bg-white" :rows="users" :columns="['username']" />
    <Pagination
      v-model="page"
      :count="userCount"
      :limit="limit"
      :defaultValue="page"
    />
  </div>
</template>

<script>
import { request } from "graphql-request";
import {
  Spinner,
  TableSimple,
  Pagination,
  MessageError,
  MessageSuccess,
  InputString,
  InputPassword,
  ButtonAction,
} from "molgenis-components";

export default {
  components: {
    Spinner,
    TableSimple,
    Pagination,
    MessageError,
    MessageSuccess,
    InputString,
    InputPassword,
    ButtonAction,
  },
  props: {
    session: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      users: [],
      userCount: null,
      loading: false,
      page: 1,
      limit: 20,
      error: null,
      username: null,
      password: null,
      alterError: null,
      alterSuccess: null,
      alterLoading: false,
      showSigninForm: true,
    };
  },
  computed: {
    offset() {
      return (this.page - 1) * this.limit;
    },
  },
  methods: {
    alterUser() {
      if (this.username == null || this.password == null) {
        this.alterError =
          "Error: valid username and password should be filled in";
      } else {
        this.alterError = null;
        this.alterLoading = true;
        request(
          "graphql",
          `mutation{changePassword(username: "${this.username}", password: "${this.password}"){status,message}}`
        )
          .then((data) => {
            if (data.changePassword.status === "SUCCESS") {
              this.alterSuccess =
                "Success. Created/altered user: " + this.username;
              this.getUserList();
            } else {
              this.alterError =
                "Create/alter user failed: " + data.changePassword.message;
            }
          })
          .catch((error) => {
            this.alterError =
              "Create/alter user failed: " + error.response.message;
          });
        this.alterLoading = false;
      }
    },
    getUserList() {
      this.loading = true;
      request(
        "graphql",
        `{_admin{users(limit:${this.limit},offset:${this.offset}){username},userCount}}`
      )
        .then((data) => {
          this.users = data._admin.users;
          this.userCount = data._admin.userCount;
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
          this.error =
            "internal error or permission denied. Did you log in as admin?";
        });
    },
  },
  watch: {
    page() {
      this.getUserList();
    },
  },
  created() {
    this.getUserList();
  },
};
</script>
