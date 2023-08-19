<template>
  <div>
    <div class="container bg-white">
      <h3>Variable Mapping</h3>
      <div class="p-0" v-for="(field, index) in fields" :key="index">
        <section-field :field="field" :color="color"></section-field>
      </div>
    </div>
  </div>
</template>

<script>
import { Client } from "molgenis-components";
import SectionField from "../components/detailView/SectionField.vue";

export default {
  components: {
    SectionField,
  },
  props: {
    target: String,
    targetDataset: String,
    targetVariable: String,
    source: String,
    sourceDataset: String,
  },
  data() {
    return { data: null, metadata: null };
  },
  computed: {
    filter() {
      return {
        source: { name: { equals: this.source } },
        sourceDataset: { name: { equals: this.sourceDataset } },
        target: { name: { equals: this.target } },
        targetDataset: { name: { equals: this.targetDataset } },
        targetVariable: { name: { equals: this.targetVariable } },
      };
    },
    fields() {
      if (this.data) {
        return this.metadata?.columns
          .filter((column) =>
            [
              "target",
              "target dataset",
              "target variable",
              "source",
              "source dataset",
              "description",
              "syntax",
            ].includes(column.name)
          )
          .map((column) => {
            return { meta: column, value: this.data[column.id] };
          });
      }
    },
  },
  methods: {
    async reload() {
      this.client = Client.newClient();
      this.metadata = await this.client.fetchTableMetaData("VariableMappings");
      this.data = (
        await this.client.fetchTableDataValues("VariableMappings", {
          filter: this.filter,
        })
      )[0];
    },
  },
  async mounted() {
    await this.reload();
  },
};
</script>
