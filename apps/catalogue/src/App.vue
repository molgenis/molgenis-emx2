<template>
  <div id="app">
    <Molgenis>
      <RouterView />
    </Molgenis>
  </div>
</template>

<script>
import { Molgenis } from "molgenis-components";

export default {
  components: {
    Molgenis,
  },
};
</script>
