<template>
  <div>
    <h2>Create/alter user</h2>
    <MessageSuccess v-if="alterSuccess">{{ alterSuccess }}</MessageSuccess>
    <MessageError v-if="alterError">{{ alterError }}</MessageError>
    <Spinner v-if="alterLoading"></Spinner>
    <form v-else class="form-inline bg-white">
      <InputString
        label="email"
        v-model="email"
        placeholder="Enter user email"
      />
      <InputPassword
        label="password"
        v-model="password"
        placeholder="Enter password"
      />
      <ButtonAction @click="alterUser" class="mt-0">Update user</ButtonAction>
    </form>
    <h2>User list</h2>
    <TableSimple
      class="bg-white"
      :rows="users"
      :columns="['email', 'enabled', 'admin']"
    >
      <template v-slot:rowheader="row">
        <template
          v-if="
            row.row.email !== 'admin' &&
            row.row.email !== 'anonymous' &&
            row.row.email !== 'user'
          "
        >
          <IconDanger
            icon="trash"
            @click="
              userToDelete = row.row.email;
              isModalShown = true;
            "
            :tooltip="`Delete ${row.row.email}`"
          />
          <IconAction
            v-if="row.row.enabled"
            icon="user-check"
            @click="disableUser(row.row.email)"
            :tooltip="`disable ${row.row.email}`"
          />
          <IconAction
            v-else
            icon="user-slash"
            @click="enableUser(row.row.email)"
            :tooltip="`re-enable ${row.row.email}`"
          />
          <IconAction
            v-if="row.row.admin"
            icon="user-tie"
            @click="setAdmin(row.row.email, false)"
            :tooltip="`revoke admin rights ${row.row.email}`"
          />
          <IconAction
            v-else
            icon="user"
            @click="setAdmin(row.row.email, true)"
            :tooltip="`grant admin rights ${row.row.email}`"
          />
        </template>
      </template>
    </TableSimple>
    <ConfirmModal
      v-if="isModalShown"
      title="Delete User"
      :actionLabel="'Delete ' + userToDelete"
      actionType="danger"
      @close="isModalShown = false"
      @confirmed="
        removeUser(userToDelete);
        userToDelete = '';
        isModalShown = false;
      "
    />

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
  IconDanger,
  IconAction,
  ConfirmModal,
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
    IconDanger,
    IconAction,
    ConfirmModal,
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
      email: null,
      password: null,
      alterError: null,
      alterSuccess: null,
      alterLoading: false,
      showSigninForm: true,
      isModalShown: false,
      userToDelete: "",
    };
  },
  computed: {
    offset() {
      return (this.page - 1) * this.limit;
    },
  },
  methods: {
    alterUser() {
      if (this.email == null || this.password == null) {
        this.alterError = "Error: valid email and password should be filled in";
      } else {
        this.alterError = null;
        this.alterLoading = true;
        request(
          "graphql",
          `mutation{changePassword(email: "${this.email}", password: "${this.password}"){status,message}}`
        )
          .then((data) => {
            if (data.changePassword.status === "SUCCESS") {
              this.alterSuccess =
                "Success. Created/altered user: " + this.email;
              this.getUserList();
            } else {
              this.alterError =
                "Create/alter user failed: " + data.changePassword.message;
            }
          })
          .catch((error) => {
            this.alterError =
              "Create/alter user failed: " + error.response.errors[0].message;
          });
        this.alterLoading = false;
      }
    },
    getUserList() {
      this.loading = true;
      request(
        "graphql",
        `{_admin{users(limit:${this.limit},offset:${this.offset}){email, enabled, admin},userCount}}`
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
    removeUser(user) {
      this.alterError = null;
      this.alterLoading = true;
      request(
        "graphql",
        `mutation{removeUser(email: "${user}"){status,message}}`
      )
        .then((data) => {
          if (data.removeUser.status === "SUCCESS") {
            this.alterSuccess = "Success. removed user: " + user;
            this.getUserList();
          } else {
            this.alterError =
              "Delete user failed: " + data.changePassword.message;
          }
        })
        .catch((error) => {
          this.alterError = "Delete user failed: " + error.response.message;
        });
      this.alterLoading = false;
    },
    setAdmin(user, isAdmin) {
      request(
        "graphql",
        `mutation updateUser($updateUser: InputUpdateUser) {
    updateUser(updateUser: $updateUser) {
      status
      message
    }
  }`,
        {
          updateUser: {
            email: user,
            admin: isAdmin,
          },
        }
      )
        .then((data) => {
          console.log(data);
          if (data.updateUser.status === "SUCCESS") {
            this.alterSuccess = "Success. updated admin permission: " + user;
            this.getUserList();
          } else {
            this.alterError =
              "Updating admin permission user failed: " +
              data.updateUser.message;
          }
        })
        .catch((error) => {
          console.log(error);
          this.alterError =
            "update user failed: " + error.response.errors[0].message;
        });
    },
    enableUser(user) {
      this.alterError = null;
      this.alterLoading = true;
      request(
        "graphql",
        `mutation{setEnabledUser(email: "${user}", enabled:true){status,message}}`
      )
        .then((data) => {
          if (data.setEnabledUser.status === "SUCCESS") {
            this.alterSuccess = "Success. enabled user: " + user;
            this.getUserList();
          } else {
            this.alterError =
              "Enable user failed: " + data.changePassword.message;
          }
        })
        .catch((error) => {
          this.alterError =
            "Granting admin rights failed: " + error.response.message;
        });
      this.alterLoading = false;
    },
    disableUser(user) {
      this.alterError = null;
      this.alterLoading = true;
      request(
        "graphql",
        `mutation{setEnabledUser(email: "${user}", enabled:false){status,message}}`
      )
        .then((data) => {
          if (data.setEnabledUser.status === "SUCCESS") {
            this.alterSuccess = "Success. disabled user: " + user;
            this.getUserList();
          } else {
            this.alterError =
              "Disable user failed: " + data.changePassword.message;
          }
        })
        .catch((error) => {
          this.alterError = "Disable user failed: " + error.response.message;
        });
      this.alterLoading = false;
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
