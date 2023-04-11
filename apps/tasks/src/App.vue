<template>
  <Molgenis id="__top" v-model="session">
    <div v-if="session && session.email == 'admin'" class="card">
      <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs">
          <li
            class="nav-item"
            v-for="(label, key) in {
              scripts: 'Scripts',
              jobs: 'Jobs',
            }"
            :key="key"
          >
            <router-link
              class="nav-link"
              :class="{
                active: selected == key,
                'text-danger': key === 'settings',
              }"
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
    <MessageError v-else>
      You have to be logged in with right permissions to see and edit settings
    </MessageError>
  </Molgenis>
</template>

<script>
import { MessageError, MessageWarning, Molgenis } from "molgenis-components";

export default {
  components: {
    Molgenis,
    MessageWarning,
    MessageError,
  },
  data() {
    return {
      session: null,
      error: null,
    };
  },
  computed: {
    selected() {
      return this.$route.name;
    },
  },
};
</script>
