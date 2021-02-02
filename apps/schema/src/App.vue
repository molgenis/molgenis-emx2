<template>
  <div id="app">
    <Molgenis id="__top" v-model="session">
      <RouterView :session="session" :key="session" />
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
