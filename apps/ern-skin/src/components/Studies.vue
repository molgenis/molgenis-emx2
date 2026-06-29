<script setup lang="ts">
import { ref } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";

// @ts-expect-error
import { MessageBox } from "molgenis-viz";

const props = defineProps<{
  table: string;
  labelsColumn?: string;
}>();

interface DataProperties {
  id?: string;
  title: string;
}

interface DataResponse {
  data: Promise<Record<string, any>>;
}

const error = ref<Error | null>(null);
const data = ref<DataProperties[]>();

async function getStudies() {
  const query = gql`query {
    ${props.table} {
      ${props.labelsColumn || ""}
    }
  }`;
  const response: DataResponse = await request("../api/graphql", query);
  data.value = response[
    props.table as keyof DataResponse
  ] as unknown as DataProperties[];
}

getStudies().catch((err) => {
  if (!err.response.errors.length) {
    error.value = err;
  } else {
    error.value = err.response.errors[0].message;
  }
});
</script>

<template>
  <MessageBox type="error" v-if="error" class="study-list-error">
    <p><strong>Unable to retrieve studies</strong></p>
    <p>{{ error }}</p>
  </MessageBox>
  <MessageBox type="error" v-else-if="!data && !error" class="study-list-error">
    <div class="p-2">
      <p>
        No studies are available. To add studies, follow the steps outlines
        below.
      </p>
      <ol>
        <li>Navigate to the <a href="../tables/#/Studies">Studies table</a></li>
        <li>
          Create a new record by clicking the add new record button located in
          the top left corner of the table.
        </li>
        <li>
          In the form that appears, enter as much information about the study as
          necessary. Make sure all required fields are completed.
        </li>
        <li>
          When you have finished, click "save". Your study will be added as a
          new record in the table.
        </li>
      </ol>
      <p>Repeat the process for each study.</p>
    </div>
  </MessageBox>
  <ul class="study" v-for="study in data">
    <li>
      <a
        class="study-element study-url"
        :href="
          '../tables/#/Studies?_filter=title&title=' +
          study[labelsColumn as keyof DataProperties] +
          '&_view=record&_limit=1'
        "
        >{{ study[labelsColumn as keyof DataProperties] }}</a
      >
    </li>
  </ul>
</template>
