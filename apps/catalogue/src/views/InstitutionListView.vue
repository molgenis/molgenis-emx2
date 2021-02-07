<template>
  <div>
    <h1>Institutes</h1>
    <p>Universities, Biobanks, Companies, Research institutions and more ...</p>
    <InstituteList />
  </div>
</template>

<script>
import InstituteList from "../components/InstituteList";

export default {
  components: { InstituteList },
};
</script>
