<template>
  <div>
    <h1>Admin tools</h1>
    <MolgenisSignin
      v-if="showSigninForm && session.email === 'anonymous'"
      @cancel="showSigninForm = false"
    />
    <Spinner v-if="loading" />
    <MessageError v-else-if="!session.admin">
      Permission denied, please log in as an administrator to view this page.
    </MessageError>
    <MessageError v-else-if="error">{{ error }}</MessageError>
    <div v-else class="card">
      <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs">
          <li
            class="nav-item"
            v-for="(label, key) in {
              users: 'Users',
              settings: 'Settings',
              privacyPolicy: 'Privacy policy',
              templates: 'Templates',
            }"
            :key="key"
          >
            <router-link
              class="nav-link"
              :class="{ active: selected == key }"
              :to="key"
              >{{ label }}
            </router-link>
          </li>
        </ul>
      </div>
      <div class="card-body">
        <router-view :session="session" />
      </div>
    </div>
  </div>
</template>

<script>
import { Spinner, MessageError, MolgenisSignin } from "molgenis-components";

export default {
  components: {
    Spinner,
    MessageError,
    MolgenisSignin,
  },
  props: {
    session: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      loading: false,
      error: null,
      showSigninForm: true,
    };
  },
  computed: {
    selected() {
      return this.$route.name;
    },
  },
};
</script>
