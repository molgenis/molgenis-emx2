<template>
  <ButtonAction v-if="show === false" @click="toggleView">
    New submission
  </ButtonAction>
  <LayoutModal
    v-else
    @close="toggleView"
    :title="'Create a new submission for schema=' + targetSchema"
  >
    <template v-slot:body>
      <Task v-if="taskId" :taskId="taskId" />
      <div v-else>
        <MessageError v-if="error">{{ error }}</MessageError>
        <InputSelect
          label="What do you want to submit?"
          description="You have here to chose a template fitting your data. When in doubt ask a data manager. In the future we intend to autogenerate the template so then it should work in all cases"
          v-model="submissionTemplate"
          name="submissionTemplate"
          :options="submissionTemplates"
          id="submissionTemplate"
        />
        <InputRefSelect
          label="Record you want to change"
          description="Here you can select the record"
          v-if="submissionTemplate"
          id="submissionId"
          v-model="submissionIdentifiers"
          :tableName="mainSubmissionTable"
        />
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAction v-if="!taskId" @click="createSubmission"
        >Create
      </ButtonAction>
    </template>
  </LayoutModal>
</template>

<script>
import {
  ButtonAction,
  LayoutModal,
  InputSelect,
  MessageError,
  Task,
  InputRefSelect,
} from "molgenis-components";
import submissionTemplates from "./submissionTemplates.json";
import { request } from "graphql-request";

export default {
  components: {
    ButtonAction,
    LayoutModal,
    InputSelect,
    MessageError,
    Task,
    InputRefSelect,
  },
  props: {
    session: Object,
    targetSchema: String,
  },
  data() {
    return {
      show: false,
      submissionTemplate: null,
      submissionIdentifiers: null,
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
    async createSubmission() {
      this.error = null;
      const response = await request(
        "graphql",
        "mutation _submissions($create:MolgenisSubmissionCreateInput){_submissions(create:$create){message,taskId}}",
        {
          create: {
            targetTables: submissionTemplates[this.submissionTemplate],
            targetIdentifiers: JSON.stringify(this.submissionIdentifiers),
          },
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
  computed: {
    mainSubmissionTable() {
      if (this.submissionTemplate) {
        return submissionTemplates[this.submissionTemplate][0];
      }
    },
    submissionTemplates() {
      //I am convinced we only would need select one target table
      //then model should be semantically so that I know what ref/ref_array to this table to include as 'partof'
      //however, for now we do it like this
      return Object.keys(submissionTemplates);
    },
    schemaList() {
      let result = this.session.schemas;
      result.sort();
      return result;
    },
  },
};
</script>
