<template>
  <div id="app">
    <Molgenis title="Settings" :menuItems="menuItems" v-model="session">
      <div v-if="session.email == undefined">
        You have to be logged in with right permissionsto see settings
      </div>
      <div v-if="session.email" class="card">
        <div class="card-header">
          <ul class="nav nav-tabs card-header-tabs">
            <li class="nav-item">
              <router-link
                class="nav-link"
                :class="{ active: selected == 'Members' }"
                to="members"
                >Members
              </router-link>
            </li>
            <li class="nav-item">
              <router-link
                class="nav-link"
                to="layout"
                :class="{ active: selected == 'Layout' }"
                >Layout
              </router-link>
            </li>
            <li class="nav-item">
              <router-link
                class="nav-link"
                to="menu"
                :class="{ active: selected == 'Menu' }"
                >Menu
              </router-link>
            </li>
            <li class="nav-item">
              <router-link
                class="nav-link"
                to="pages"
                :class="{ active: selected == 'Pages' }"
                >Pages
              </router-link>
            </li>
          </ul>
        </div>
        <div class="card-body">
          <router-view :session="session" />
        </div>
      </div>
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Molgenis
  },
  data() {
    return {
      session: {}
    };
  },
  computed: {
    selected() {
      return this.$route.name;
    },
    menuItems() {
      return [
        { label: "Tables", href: "../tables/" },
        {
          label: "Schema",
          href: "../schema/"
        },
        {
          label: "Upload",
          href: "../import/"
        },
        {
          label: "Download",
          href: "../download/"
        },
        {
          label: "GraphQL",
          href: "/api/playground.html?schema=/api/graphql/" + this.schema
        },
        {
          label: "Settings",
          href: "../settings/"
        }
      ];
    }
  }
};
</script>
