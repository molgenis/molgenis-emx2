<template>
  <ProviderDashboard id="provider-clp">
    <h2>Surgical overview for all centers</h2>
  </ProviderDashboard>
</template>

<script setup>
import ProviderDashboard from "../components/ProviderDashboard.vue";

const props = defineProps({
  user: String,
  organization: Object
})

</script>