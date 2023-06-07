<template>
  <span v-if="loading"><Spinner></Spinner></span>
  <span v-else-if="update">
    <a :href="`/${this.schema}/settings/#/changelog`">
      {{ formatStamp(update.stamp) }} ({{ update.tableName }})
    </a>
  </span>
  <span v-else
    ><em>{{ statusMessage }}</em></span
  >
</template>

<script>
import { request } from "graphql-request";
import { Spinner } from "molgenis-components";
export default {
  name: "LastUpdateField",
  components: { Spinner },
  props: {
    schema: {
      String,
      required: true,
    },
  },
  data() {
    return {
      update: null,
      loading: true,
      statusMessage: "",
    };
  },
  methods: {
    formatStamp(stamp) {
      const date = new Date(stamp);
      return date.toLocaleDateString();
    },
  },
  async created() {
    const resp = await request(
      `/${this.schema}/settings/graphql`,
      "{_changes(limit: 1) {operation, stamp, userId, tableName}}"
    ).catch(error => {
      this.statusMessage = "failed to fetch updated";
      console.log(error);
    });

    this.update = resp ? resp["_changes"][0] : null;

    if (this.update && this.update.stamp) {
      // emit timestamp for use in row sort
      this.$emit("input", this.update.stamp);
    }
    this.loading = false;
  },
};
</script>
