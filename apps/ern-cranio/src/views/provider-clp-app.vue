<template>
  <router-view :user="user" :organization="organization"></router-view>
</template>

<script setup>
const props = defineProps({
  user: String,
  organization: Object,
});
</script>
  