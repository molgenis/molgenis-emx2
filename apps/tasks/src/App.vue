<template>
  <Molgenis id="__top" v-model="session">
    <MessageWarning v-if="error">{{ error }}</MessageWarning>
    <router-view v-else :session="session" :key="JSON.stringify(session)" />
  </Molgenis>
</template>

<script>
import { Molgenis, MessageWarning } from "molgenis-components";

export default {
  components: {
    Molgenis,
    MessageWarning,
  },
  data() {
    return {
      session: null,
      error: null,
    };
  },
};
</script>
