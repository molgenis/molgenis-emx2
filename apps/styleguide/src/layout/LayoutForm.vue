<template>
  <Panel outside>
    <form><slot /></form>
  </Panel>
</template>

<script>
import Panel from "../layout/Panel";
export default {
  components: {
    Panel
  }
};
</script>
