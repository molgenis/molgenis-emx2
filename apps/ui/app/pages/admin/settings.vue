<script setup lang="ts">
import { definePageMeta } from "#imports";

definePageMeta({
  middleware: "admin-only",
});
</script>
<template>Settings</template>
