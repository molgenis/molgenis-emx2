<template>
  <div>
    <IconAction v-if="!isModalShown" icon="search" @click="open" />
    <LayoutModal v-else title="Job details" :show="isModalShown" @close="close">
      <template #body>
        <Task v-if="taskId" :taskId="taskId" />
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
    };
  },
  methods: {
    open() {
      this.isModalShown = true;
    },
    close() {
      this.isModalShown = false;
    },
  },
};
</script>
