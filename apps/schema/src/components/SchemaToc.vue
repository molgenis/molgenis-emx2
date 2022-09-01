<template>
  <div class="sticky-top mr-n3" style="top: 50px">
    <h4>
      Tables
      <IconAction icon="plus" @click="addTable" />
    </h4>
    <ul v-if="schema.tables" class="overflow-auto 90-vh" style="height: 90vh">
      <li v-for="table in schema.tables" :key="table.name">
        <a
          :href="'#'"
          v-scroll-to="{
            el: '#' + (table.name ? table.name.replaceAll(' ', '_') : ''),
            offset: -50,
          }"
          >{{ table.name }}</a
        >
        <ul v-if="table.subclasses">
          <li v-for="subtable in table.subclasses" :key="subtable.name">
            <a
              :href="'#'"
              v-scroll-to="{
                el:
                  '#' +
                  (subtable.name ? subtable.name.replaceAll(' ', '_') : ''),
                offset: -50,
              }"
              >{{ subtable.name }}</a
            >
          </li>
        </ul>
      </li>
    </ul>
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
