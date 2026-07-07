<script setup lang="ts">
import { ref } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";

// @ts-expect-error
import { MessageBox } from "molgenis-viz";
import type { IPublications } from "../../../metadata-utils/src/viz/ErnDashboard";

const props = defineProps<{
  table: string;
  labelsColumn?: string;
  doiColumn?: string;
}>();

const error = ref<Error | null>(null);
const data = ref<IPublications[]>([]);

async function getPublications() {
  const query = gql`query {
    ${props.table} {
      ${props.labelsColumn || ""}
      ${props.doiColumn}
    }
  }`;
  const response: Record<string, any> = await request("../api/graphql", query);
  data.value = response[props.table];
}

getPublications().catch((err) => {
  error.value = err.response?.errors[0].message || err;
});
</script>

<template>
  <MessageBox type="error" v-if="error" class="publication-list-error">
    <p><strong>Unable to retrieve publications</strong></p>
    <p>{{ error }}</p>
  </MessageBox>
  <MessageBox
    type="error"
    v-else-if="!data && !error"
    class="publication-list-error"
  >
    <div class="p-2">
      <p>
        No publications are available. To add publications, follow the steps
        outlines below.
      </p>
      <ol>
        <li>
          Navigate to the
          <a href="../tables/#/Publications">Publications table</a>
        </li>
        <li>
          Create a new record by clicking the add new record button located in
          the top left corner of the table.
        </li>
        <li>
          In the form that appears, enter as much information about the
          publication as necessary. Make sure all required fields are completed.
        </li>
        <li>
          When you have finished, click "save". Your publication will be added
          as a new record in the table.
        </li>
      </ol>
      <p>Repeat the process for each publication.</p>
    </div>
  </MessageBox>
  <ul class="publication" v-for="publication in data">
    <li>
      <a
        class="publication-element publication-url"
        :href="publication[doiColumn as keyof IPublications]"
        target="_blank"
      >
        {{ publication[labelsColumn as keyof IPublications] }}
      </a>
    </li>
  </ul>
</template>
