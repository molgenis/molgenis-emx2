<template>
  <div id="app">
    <Molgenis
      v-model="session"
      :key="JSON.stringify(session)"
      @error="handleError"
    >
      <Spinner v-if="!error && !session" />
      <MessageWarning v-else-if="error && !session.roles">{{
        error
      }}</MessageWarning>
      <div v-else class="container-fluid">
        <RouterView />
      </div>
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis, Spinner, MessageWarning } from "molgenis-components";

export default {
  components: {
    Molgenis,
    Spinner,
    MessageWarning,
    GTag,
  },
  data() {
    return {
      session: {},
      error: null,
      analyticsId: null,
    };
  },
  methods: {
    handleError(error) {
      this.error =
        typeof error === "string"
          ? error
          : "An error occurred while trying to load the session data";
    },
  }
};
</script>
