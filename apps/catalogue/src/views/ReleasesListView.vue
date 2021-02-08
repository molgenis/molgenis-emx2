<template>
  <div class="container-fluid">
    <h1>Releases</h1>
    <p>Versioned releases from all resources.</p>
    <ReleasesList />
  </div>
</template>

<script>
import ReleasesList from "../components/ReleasesList";

export default {
  components: { ReleasesList },
};
</script>
