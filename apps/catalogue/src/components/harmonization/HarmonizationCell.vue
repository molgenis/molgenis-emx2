<template>
  <td class="text-center" :class="tableClass">
    <i class="fa fa-fw" :class="iconClass" />
  </td>
</template>

<script>
export default {
  name: "HarmonicationCell",
  props: {
    status,
  },
  computed: {
    iconClass() {
      switch (this.status) {
        case "unmapped":
          return "fa-none";
        case "partial":
          return "fa-percent";
        case "complete":
          return "fa-check";
        default:
          return "fa-none";
      }
    },
    tableClass() {
      switch (this.status) {
        case "unmapped":
          return "table-light";
        case "partial":
          return "table-warning";
        case "complete":
          return "table-success";
        default:
          return "table-light";
      }
    },
  },
};
</script>

<style>
td {
  padding: 0.97rem;
}
</style>
