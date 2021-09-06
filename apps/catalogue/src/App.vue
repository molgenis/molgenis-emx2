<template>
  <div id="app">
    <Molgenis v-model="session" :key="JSON.stringify(session)">
      <Spinner v-if="!session || !session.roles" />
      <div v-else-if="!session.roles || !session.roles.includes('Viewer')">
        <h1 class="text-centered">You have to login to view the data.</h1>
      </div>
      <div v-else class="container-fluid">
        <RouterView :session="session" />
      </div>
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis, Spinner } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Molgenis,
    Spinner,
  },
  data() {
    return {
      session: {},
    };
  },
};
</script>
