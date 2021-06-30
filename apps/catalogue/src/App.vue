<template>
  <div id="app">
    <Molgenis v-model="session" :key="JSON.stringify(session)">
      <template v-slot:breadCrumb>
        <bread-crumbs :crumbs="breadCrumbs"></bread-crumbs>
      </template>
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
import BreadCrumbs from "../../styleguide/src/layout/BreadCrumbs.vue";
import { mapState } from "vuex";

export default {
  components: {
    Molgenis,
    BreadCrumbs,
  },
  data() {
    return {
      session: {},
    };
  },
  computed: {
    ...mapState(["breadCrumbs"]),
  },
};
</script>
