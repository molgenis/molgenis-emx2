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
    filter='(filter: { tags: { equals: "private" } })'
    labelsColumn="name"
    fileColumn="file"
   />
   </p>
</template>

<script setup lang="ts">
// @ts-ignore
import { FileList, MessageBox } from "molgenis-viz";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { ref, onMounted } from 'vue'

const error = ref<Error | null>(null);
const loading = ref(true);
const user = ref<string | null>(null);

async function getSession() {
  const query = gql`
    query {
      _session {
        email
      }
    }
  `
  const response = await request('/api/graphql', query, {}, { credentials: 'include' })
  user.value = response._session?.email || null
}
onMounted(() => {
getSession()
  .catch((err: any) => {
    if (err.response?.errors?.length) {
      error.value = err.response.errors[0].message
    } else {
      error.value = err
    }
  })
  .finally(() => {
    loading.value = false
  });
  });
</script>