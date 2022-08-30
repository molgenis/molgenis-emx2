<template>
  <div class="sticky-top mr-n3" style="top: 50px">
    <h4>
      Tables
      <IconAction icon="plus" @click="addTable" />
    </h4>
    <div v-if="schema.tables" class="overflow-auto 90-vh" style="height: 90vh">
      <p
        v-for="table in schema.tables.filter(
          (t) => t.externalSchema === undefined && t.inherit === undefined
        )"
        :key="table.name"
      >
        <a
          :href="'#'"
          v-scroll-to="{
            el: '#' + (table.name ? table.name.replaceAll(' ', '_') : ''),
            offset: -50,
          }"
          >{{ table.name }}</a
        >
      </p>
    </div>
  </div>
</template>

<script>
import Vue from "vue";
import VueScrollTo from "vue-scrollto";
import { IconAction } from "molgenis-components";

Vue.use(VueScrollTo);

export default {
  components: { IconAction },
  props: {
    /** schema v-model */
    value: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      schema: null,
    };
  },
  methods: {
    addTable() {
      this.$scrollTo("#molgenis_bottom_page_anchor");
      this.schema.tables.push({ name: undefined, columns: [] });
      this.$emit("input", this.schema);
    },
  },
  created() {
    this.schema = this.value;
  },
};
</script>
