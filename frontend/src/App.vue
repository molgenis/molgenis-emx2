<template>
  <div id="app">
    <Molgenis
      v-model="session"
      :menu-items="menuItems"
      title="Welcome to MOLGENIS"
    >
      <Groups :key="JSON.stringify(session)" :session="session" />
      <!-- from catalogue App.vue -->
      <div class="container-fluid">
        <RouterView :key="$route.fullPath" :page="page" :session="session" />
      </div>
    </Molgenis>
  </div>
</template>

<script>
import Groups from './components/central/Groups.vue'
import Molgenis from './components/ui/layout/Molgenis.vue'

export default {
  components: {
    Groups,
    Molgenis,
  },
  data: function() {
    return {
      menuItems: [
        {active: true, href: '.', label: 'Databases'},
        {
          href: '/apps/graphql-playground/',
          label: 'GraphQL API',
        },
        {
          href: '/apps/styleguide/',
          label: 'Styleguide',
        },
        {
          href: '/apps/docs/',
          label: 'Docs',
          newWindow: true,
        },
      ],
      page: null,
      session: {},
    }
  },
}
</script>
