<template>
  <div id="app">
    <router-view v-if="state" :state="state" />
    <div v-else>
      <!-- below only for development purposes-->
      <div v-if="filterError">
        <h1>Error: filter malformed</h1>
        <div>Value provided: {{ $route.params.filter }}</div>
        <div>Error: {{ filterError }}</div>
      </div>
      <ClientSideStateLoader
        :table="table"
        :filter="filter"
        :key="$route.fullPath"
      >
        <template v-slot:default="slotProps">
          <router-view :state="slotProps.state" />
        </template>
      </ClientSideStateLoader>
    </div>
  </div>
</template>

<script>
import ClientSideStateLoader from "./components/ClientSideStateLoader";

export default {
  name: "App",
  components: { ClientSideStateLoader },
  data() {
    return {
      filterError: null,
    };
  },
  props: {
    state: {
      type: Object,
      default: () => {
        //check if we have globally defined state via the ssr
        //otherwise it stays null
        if (typeof state !== "undefined") {
          return JSON.parse(state);
        } else {
          return null;
        }
      },
    },
  },
  computed: {
    table() {
      //either from props or params
      if (this.$route.params.table) {
        return this.$route.params.table;
      } else if (this.$route.matched[0].props.default.table) {
        return this.$route.matched[0].props.default.table;
      }
    },
    filter() {
      if (this.$route.params.filter) {
        try {
          return JSON.parse(this.$route.params.filter);
        } catch (e) {
          this.filterError = e;
        }
      } else {
        return this.$route.matched[0].props.default.filter;
      }
    },
  },
};
</script>
