<template>
  <div id="app">
    <Molgenis id="__top" v-model="session">
      <RouterView :session="session" :key="session.email" />
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
};
</script>
