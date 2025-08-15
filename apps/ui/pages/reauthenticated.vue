<script setup lang="ts">
import { definePageMeta } from "#imports";

definePageMeta({
  layout: false, // disables all layouts
});

window.opener.postMessage(
  { status: "reAuthenticated" },
  window.location.origin // restrict to same origin
);
setTimeout(() => {
  window.close();
}, 3000);
</script>

<template>
  <h1>Re-authenticated, this window will close automatically.</h1>
</template>
