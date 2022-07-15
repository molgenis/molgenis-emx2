<template>
  <div>
    <h1>Changes</h1>
    <p v-if="changesCount">{{changesCount}} changes where made</p>
    <table-display :columns="columns" :rows="changes" />
  </div>
</template>

<script>
import { request } from "graphql-request";
import { TableDisplay } from "@mswertz/emx2-styleguide";

export default {
  name: "LogViewer",
  components: { TableDisplay },
  data() {
    return {
      columns: [
        { name: "operation", label: "Action" },
        { name: "tableName", label: "Table" },
        { name: "userId", label: "Who" },
        { name: "stamp", label: "When" },
        { name: "oldRowData", label: "Old" },
        { name: "newRowData", label: "New" },
      ],
      changes: [],
      changesCount: null
    };
  },
  methods: {
    async fetchChanges() {
      const resp = await request(
        "graphql",
        `{_changes {operation, stamp, userId, tableName, oldRowData, newRowData} _changesCount}`
      );
      this.changes = resp._changes
        .map(this.formatOperation)
        .map(this.formatDateTime)
        .map(this.formatRowData);
      this.changesCount = resp._changesCount
    },
    formatOperation(change) {
      if (change.operation === "I") {
        change.operation = "insert";
      } else if (change.operation === "U") {
        change.operation = "update";
      } else if (change.operation === "D") {
        change.operation = "delete";
      } else {
        change.operation = "unknown action";
      }

      return change;
    },
    formatDateTime(change) {
      change.timeStamp = new Date(change.timeStamp).toLocaleString();
      return change;
    },
    formatRowData(change) {
      change.oldRowData = change.oldRowData ? JSON.stringify(change.oldRowData, null, 2) : '-';
      change.newRowData = change.newRowData ? JSON.stringify(change.newRowData, null, 2) : '-';
      return change;
    },
  },
  mounted: function () {
    this.fetchChanges();
  },
};
</script>

<style>
</style>
