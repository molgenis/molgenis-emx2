<template>
  <MessageBox type="error" v-if="error" class="file-list-error">
    <p><strong>Unable to get active user: </strong></p>
    <p>{{ error }}</p>
  </MessageBox>
  <MessageBox type="error" v-else-if="!user && !error" class="study-list-error">
    <p><strong>No active user found</strong></p>
  </MessageBox>
  <p v-if="user !== 'anonymous' && user">
    <FileList
      table="Files"
      :filter="filterArgument"
      labelsColumn="name"
      fileColumn="file"
      :key="filterKey"
    />
  </p>
</template>

<script setup lang="ts">
// @ts-ignore
import { FileList, MessageBox } from "molgenis-viz";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { ref, onMounted, computed } from "vue";

const props = defineProps<{ labelValue?: string }>();

const filterArgument = computed(() => {
  let filter = 'filter: { tags: { equals: "private" }';
  if (props.labelValue) {
    filter += `, label: { equals: "${props.labelValue}" }`;
  }

  filter += " }";

  return filter;
});

const error = ref<Error | null>(null);
const loading = ref(true);
const user = ref<string | null>(null);

// Special key to re-trigger getting data if labelFilter part changes
const filterKey = computed(() => JSON.stringify(filterArgument.value));

async function getSession() {
  const query = gql`
    query {
      _session {
        email
      }
    }
  `;
  const response = await request(
    "/api/graphql",
    query,
    {},
    { credentials: "include" }
  );
  user.value = response._session?.email || null;
}

getSession()
  .catch((err: any) => {
    if (err.response?.errors?.length) {
      error.value = err.response.errors[0].message;
    } else {
      error.value = err;
    }
  })
  .finally(() => {
    loading.value = false;
  });
</script>
