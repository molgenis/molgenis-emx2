<template>
  <div id="app">
    <Molgenis
      v-model="session"
      :key="JSON.stringify(session)"
      :showCrumbs="showCrumbs"
    >
      <div
        v-if="!session || !session.roles || !session.roles.includes('Viewer')"
      >
        <h1 class="text-centered">You have to login to view the data.</h1>
      </div>
      <div v-else class="container-fluid">
        <RouterView />
      </div>
    </Molgenis>
  </div>
</template>

<script>
import Molgenis from "../../styleguide/src/layout/Molgenis.vue";

export default {
  components: {
    Molgenis,
  },
  data() {
    return {
      session: {},
      showCrumbs: false,
    };
  },
};
</script>
