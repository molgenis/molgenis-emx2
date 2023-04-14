<template>
  <div>
    <IconAction v-if="!isModalShown" icon="play" @click="open" />
    <LayoutModal
      v-else
      title="Submit script"
      :show="isModalShown"
      @close="close"
    >
      <template #body>
        <p v-if="!taskId">
          todo: parameters dialogue.
          <MessageError v-if="error">{{ error }}</MessageError>
          <Task v-if="taskId" :taskId="taskId" />
          <router-link v-if="!taskId" to="jobs">View all jobs</router-link>
        </p></template
      >

      <template #footer>
        <ButtonAction v-if="!taskId" @click="submitScript">Submit</ButtonAction>
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
  MessageError,
  Task,
} from "molgenis-components";

export default {
  components: {
    IconAction,
    LayoutModal,
    ButtonAction,
    ButtonAlt,
    Task,
    MessageError,
  },
  props: {
    script: {
      type: Object,
      required: true,
    },
  },
  data() {
    return {
      taskId: null,
      error: null,
      success: null,
      loading: false,
      isModalShown: false,
    };
  },
  methods: {
    open() {
      this.isModalShown = true;
    },
    close() {
      this.taskId = null;
      this.isModalShown = false;
    },
    submitScript() {
      let url = "/api/task?async=true&name=" + this.script.name;
      fetch(url, {
        method: "POST",
      })
        .then((response) => {
          if (response.ok) {
            response.json().then((task) => {
              this.taskId = task.id;
              this.error = null;
            });
          } else {
            response.json().then((error) => {
              this.success = null;
              this.error = error.errors[0].message;
            });
          }
        })
        .catch((error) => {
          this.error = error;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>
