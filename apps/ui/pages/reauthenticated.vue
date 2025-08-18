<script setup lang="ts">
import { definePageMeta } from "#imports";

definePageMeta({
  layout: false, // disables all layouts
});

window?.opener?.postMessage(
  { status: "reAuthenticated" },
  window.location.origin // restrict to same origin
);
setTimeout(() => {
  if (window?.opener) {
    window.close();
  }
}, 3000);

if (!window.opener) {
  window.location.href = "/";
}
</script>

<template>
  <h1 class="text-center p-4 text-heading-xl">
    Re-authenticated, this window will close automatically.
  </h1>
</template>
