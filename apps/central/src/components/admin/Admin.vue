<template>
  <div>
    <h1>Admin tools</h1>
    <Spinner v-if="loading"></Spinner>
    <MessageError v-else-if="error">{{ error }}</MessageError>
    <div v-else>
      <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs">
          <li
            class="nav-item"
            v-for="(label, key) in {
              users: 'Users',
              settings: 'Settings',
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
import { Spinner, MessageError } from "molgenis-components";

export default {
  components: {
    Spinner,
    MessageError,
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
    };
  },
  computed: {
    selected() {
      return this.$route.name;
    },
  },
  created() {
    if (this.session.email !== "admin") {
      this.$router.push("/");
    }
  },
};
</script>
