<template>
  <div id="app">
    <Molgenis v-model="session" :key="JSON.stringify(session)">
      <div
        v-if="!session || !session.roles || !session.roles.includes('Viewer')"
      >
        <h1 class="text-centered">You have to login to view the data.</h1>
      </div>
      <div v-else>
        <div class="container-fluid">
          <RouterView />
        </div>
      </div>
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Molgenis,
  },
  data() {
    return {
      session: {},
    };
  },
};
</script>
