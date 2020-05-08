<template>
  <Molgenis :menuItems="menuItems" :title="title" id="__top">
    <ListTables />
  </Molgenis>
</template>

<script>
import { Molgenis } from "@mswertz/emx2-styleguide";
import ListTables from "./components/ListTables";

export default {
  name: "App",
  components: {
    Molgenis,
    ListTables
  },
  data() {
    return {
      menuItems: [{ label: "Home", href: "." }]
    };
  }
};
</script>
