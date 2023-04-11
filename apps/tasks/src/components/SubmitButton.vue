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
        todo {{ taskId }}
        <Task v-if="taskId" :taskId="taskId" />
      </template>
      <template #footer>
        <ButtonAction @click="submitScript">Submit</ButtonAction>
        <ButtonAlt @click="close">Cancel</ButtonAlt>
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
  Task,
} from "molgenis-components";

export default {
  components: {
    IconAction,
    LayoutModal,
    ButtonAction,
    ButtonAlt,
    Task,
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
