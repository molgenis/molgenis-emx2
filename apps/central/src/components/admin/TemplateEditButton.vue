<template>
  <div>
    <IconAction v-if="!isModalShown" icon="search" @click="open" />
    <LayoutModal
      v-else
      title="Edit template"
      :show="isModalShown"
      @close="close"
    >
      <template #body>
        <InputSelect :options="schemas"></InputSelect>
      </template>
      <template #footer>
        <ButtonAlt @click="close">Close</ButtonAlt>
      </template>
    </LayoutModal>
  </div>
</template>
<script>
import {
  ButtonAction,
  ButtonAlt,
  IconAction,
  LayoutModal,
  InputSelect,
} from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    IconAction,
    LayoutModal,
    ButtonAction,
    ButtonAlt,
    InputSelect,
  },
  props: {
    taskId: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      error: null,
      success: null,
      loading: false,
      isModalShown: false,
      schemas: [],
      graphqlError: null,
    };
  },
  created() {
    this.getSchemaList();
  },
  methods: {
    getSchemaList() {
      this.loading = true;
      request("graphql", `{_schemas{id,label,description}}`)
        .then((data) => {
          this.schemas = data._schemas;
          this.loading = false;
        })
        .catch((error) => {
          console.error("internal server error", error);
          this.graphqlError = "internal server error" + error;
          this.loading = false;
        });
    },
    open() {
      this.isModalShown = true;
    },
    close() {
      this.isModalShown = false;
    },
  },
};
</script>
