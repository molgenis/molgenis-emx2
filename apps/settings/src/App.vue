<template>
  <div id="app">
    <Molgenis title="Settings" v-model="session">
      <div
        v-if="
          session.email == 'admin' ||
          (session.roles && session.roles.includes('Manager'))
        "
        class="card"
      >
        <div class="card-header">
          <ul class="nav nav-tabs card-header-tabs">
            <li
              class="nav-item"
              v-for="(label, key) in {
                members: 'Members',
                theme: 'Theme',
                menu: 'Menu',
                pages: 'Pages',
                changelog: 'Changelog',
                settings: 'Advanced settings',
              }"
              :key="key"
            >
              <router-link
                class="nav-link"
                :class="{
                  active: selected == label,
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
      <div v-else>
        You have to be logged in with right permissions to see and edit settings
      </div>
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "molgenis-components";

export default {
  components: {
    Molgenis,
  },
  data() {
    return {
      session: {},
    };
  },
  computed: {
    selected() {
      return this.$route.name;
    },
  },
};
</script>
