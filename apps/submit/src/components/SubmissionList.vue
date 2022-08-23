<template>
  <div class="container">
    <Spinner v-if="loading" />
    <MessageError v-else-if="error">{{ error }}</MessageError>
    <div v-else class="row bg-white">
      <div class="col">
        <h2>Submissions</h2>
        <p>
          Below a list of submissions for schema='{{ schema.name }}' and their
          status.
        </p>
        <div class="m-2">
          <SubmissionCreate
            :session="session"
            :targetSchema="schema.name"
            @close="reload"
          />
        </div>
        <TableSimple
          :rows="submissions"
          :columns="['id', 'status', 'created', 'changed']"
        >
          <template #rowheader="rowProps">
            <a
              v-if="rowProps.row.status == 'DRAFT'"
              class="btn btn-primary"
              :href="'/Submit_' + rowProps.row.id"
            >
              view
            </a>
            <SubmissionMerge
              class="ml-2"
              v-if="rowProps.row.status === 'DRAFT'"
              :id="rowProps.row.id"
              :session="session"
              @close="reload"
            />
          </template>
        </TableSimple>
      </div>
    </div>
  </div>
</template>

<script>
import SubmissionCreate from "./SubmissionCreate.vue";
import SubmissionMerge from "./SubmissionMerge.vue";
import { request } from "graphql-request";
import { TableSimple, Spinner, MessageError } from "molgenis-components";

export default {
  components: {
    SubmissionCreate,
    TableSimple,
    Spinner,
    MessageError,
    SubmissionMerge,
  },
  props: {
    session: Object,
  },
  data() {
    return {
      submissions: [],
      client: null,
      schema: null,
      loading: true,
      error: null,
    };
  },
  methods: {
    async reload() {
      this.loading = true;
      const response = await request(
        "graphql",
        "{_submissions{id,status,created,changed},_schema{name}}"
      ).catch(this.handleError);
      if (response) {
        this.submissions = response._submissions.map((submission) => {
          submission.created = submission.created.split(".")[0];
          submission.changed = submission.changed.split(".")[0];
          return submission;
        });
        this.schema = response._schema;
      }
      this.loading = false;
    },
    handleError(error) {
      if (Array.isArray(error?.response?.errors)) {
        this.error = error.response.errors[0].message;
      } else {
        this.error = error;
      }
      this.loading = false;
    },
  },
  created() {
    this.reload();
  },
};
</script>
