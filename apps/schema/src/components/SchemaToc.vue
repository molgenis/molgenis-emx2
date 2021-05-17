<template>
  <div>
    <h4>
      Tables
      <IconAction icon="plus" @click="addTable" />
    </h4>
    <div v-if="tables">
      <p
        v-for="table in tables.filter((t) => t.externalSchema == undefined)"
        :key="table.name"
      >
        <a v-scroll-to="'#' + table.name" href=".">{{ table.name }}</a>
      </p>
    </div>
  </div>
</template>

<script>
import Vue from "vue";
import VueScrollTo from "vue-scrollto";
import { IconAction } from "@mswertz/emx2-styleguide";

Vue.use(VueScrollTo);

export default {
  components: { IconAction },
  props: {
    tables: Array,
  },
  methods: {
    addTable() {
      let result = [];
      if (this.tables) {
        result = this.tables;
      }
      let name = "NewTable";
      result.unshift({
        name: name,
        columns: [],
      });
      this.$emit("update:tables", result);
    },
  },
};
</script>
