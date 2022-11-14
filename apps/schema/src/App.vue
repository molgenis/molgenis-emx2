<template>
  <Molgenis id="__top" v-model="session">
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
