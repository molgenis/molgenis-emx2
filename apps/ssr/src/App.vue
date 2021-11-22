<template>
  <div>
    <router-view v-if="state" :state="state" />
    <div v-else>
      <!--
      Todo: get document is not defined error
      PreviewWrapper :table="table" :filter="filter">
        <template v-slot:default="slotProps">
          <router-view :state="slotProps.state" />
        </template>
      </PreviewWrapper-->
    </div>
  </div>
</template>

<script>
//import PreviewWrapper from "./components/PreviewWrapper";

export default {
  //  components: { PreviewWrapper },
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
      return this.$route.params.table;
    },
    filter() {
      if (this.$route.params.filter) {
        return { equals: JSON.parse(this.$route.params.filter) };
      }
    },
  },
};
</script>
