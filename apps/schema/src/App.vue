<template>
    <Molgenis  id="__top" v-model="session">
      <router-view :session="session" :key="session.email" />
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
      session: {},
    };
  }
};
</script>
