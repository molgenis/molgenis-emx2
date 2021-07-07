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
          return "fa-times";
        case "partial":
          return "fa-percent";
        case "complete":
          return "fa-check";
        default:
          return "fa-times";
      }
    },
    tableClass() {
      switch (this.status) {
        case "unmapped":
          return "table-danger";
        case "partial":
          return "table-warning";
        case "complete":
          return "table-success";
        default:
          return "table-danger";
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
