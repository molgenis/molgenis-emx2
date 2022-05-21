<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" />
  </Molgenis>
</template>

<script>
import { Molgenis } from "molgenis-components";

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
