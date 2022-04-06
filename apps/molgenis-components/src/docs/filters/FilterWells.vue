<template>
  <demo-item id="filter-wells" label="Filter wells">
    <FilterWells :filters.sync="filters" />
    value: {{ JSON.stringify(filters) }}
  </demo-item>
</template>
<script>
export default {
  data: function () {
    return {
      filters: [
        {
          name: "orderId",
          conditions: [123],
        },
        {
          name: "pet",
          conditions: [],
        },
        {
          name: "quantity",
          conditions: [[1, null]],
        },
        {
          name: "price",
          conditions: [
            [null, 25.7],
            [2, 11.3],
          ],
        },
        {
          name: "complete",
          conditions: ["false"],
        },
        {
          name: "status",
          conditions: [{ name: "status", second: " true" }],
        },
        {
          name: "birthday",
          conditions: [["2020-1-1", "2030-2-3"]],
        },
      ],
    };
  },
  methods: {
    showAlert() {
      alert("clicked");
    },
  },
};
</script>