<template>
  <table class="table bg-white table-hover">
    <thead>
      <tr>
        <th scope="col">Label</th>
        <th scope="col">Description</th>
      </tr>
    </thead>
    <tr v-for="table in tables" :key="table.id">
      <td>
        <router-link :to="table.id">{{ table.label }}</router-link>
      </td>
      <td>{{ table.description }}</td>
    </tr>
  </table>
</template>

<script>
import {
  getLocalizedDescription,
  getLocalizedLabel,
} from "molgenis-components";

export default {
  props: {
    tables: Array,
    locale: {
      type: String,
      default: "en",
    },
  },
  methods: {
    getLocalizedDescription(table) {
      return getLocalizedDescription(table, this.locale);
    },
    getLocalizedLabel(table) {
      return getLocalizedLabel(table, this.locale);
    },
  },
};
</script>
