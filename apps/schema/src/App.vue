<template>
  <div v-if="$route.path == '/print' || $route.path == '/print-list'">
    <router-view />
  </div>
  <Molgenis id="__top" v-model="session" v-else>
    <Suspense>
      <router-view :session="session" />
    </Suspense>
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
