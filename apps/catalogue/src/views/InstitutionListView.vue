<template>
  <div>
    <h1>Institutions</h1>
    <p>Universities, Biobanks, Companies, Research institutions and more ...</p>
    <InstituteList />
  </div>
</template>

<script>
import InstitutionList from "../components/InstitutionList";

export default {
  components: { InstituteList: InstitutionList },
};
</script>
