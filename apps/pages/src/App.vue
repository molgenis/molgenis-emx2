<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
  </Molgenis>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";

export default {
  components: {
    Molgenis
  },
  data() {
    return {
      session: null,
      page: null
    };
  }
};
</script>
