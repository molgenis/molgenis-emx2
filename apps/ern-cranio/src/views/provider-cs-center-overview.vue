<template>
  <ProviderDashboard class="two-column-layout">
    <h2>General overview for your center</h2>
    
  </ProviderDashboard>
</template>

<script setup>
import ProviderDashboard from "../components/ProviderDashboard.vue";

const props = defineProps({
  user: String,
  organization: Object
})


</script>