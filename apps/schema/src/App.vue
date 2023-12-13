<template>
  <div v-if="$route.path == '/print' || $route.path == '/print-list'">
    <router-view />
  </div>
  <Molgenis id="__top" v-model="session" v-else>
    <router-view :session="session" />
  </Molgenis>
</template>

<script>
import { Molgenis } from "molgenis-components";

export default {
  components: {
    Molgenis,
  },
  data() {
    return {
      session: null,
    };
  },
};
</script>
