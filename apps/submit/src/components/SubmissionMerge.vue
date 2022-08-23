<template>
  <ButtonAction v-if="show === false" @click="merge"> Merge</ButtonAction>
  <LayoutModal v-else @close="toggleView">
    <template v-slot:body>
      <h2>Merge submission={{ id }}</h2>
      <MessageError v-if="error">{{ error }}</MessageError>
      <Task v-if="taskId" :taskId="taskId" />
    </template>
  </LayoutModal>
</template>

<script>
import {
  ButtonAction,
  LayoutModal,
  Task,
  MessageError,
} from "molgenis-components";
import { request } from "graphql-request";

export default {
  components: {
    ButtonAction,
    LayoutModal,
    Task,
    MessageError,
  },
  props: {
    session: Object,
    id: String,
  },
  data() {
    return {
      show: false,
      taskId: null,
      error: null,
    };
  },
  methods: {
    toggleView() {
      this.show = !this.show;
      if (!this.show) {
        this.taskId = null;
        this.$emit("close");
      }
    },
    async merge() {
      this.show = true;
      this.error = null;
      const response = await request(
        "graphql",
        "mutation _submissions($id:String){_submissions(merge:$id){message,taskId}}",
        {
          id: this.id,
        }
      ).catch(this.handleError);
      this.taskId = response._submissions.taskId;
    },
    handleError(error) {
      if (Array.isArray(error?.response?.data?.errors)) {
        this.error = error.response.data.errors[0].message;
      } else {
        this.error = error;
      }
      this.loading = false;
    },
  },
};
</script>
