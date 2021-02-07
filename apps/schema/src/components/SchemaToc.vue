<template>
  <div>
    <h1>Schema:</h1>
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

Vue.use(VueScrollTo);

export default {
  props: {
    tables: Array,
  },
};
</script>
