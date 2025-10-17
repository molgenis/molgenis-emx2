<template>
  <MessageBox type="error" v-if="error" class="publication-list-error">
    <p><strong>Unable to retrieve publications</strong></p>
    <p>{{ error }}</p>
  </MessageBox>
  <MessageBox
    type="error"
    v-else-if="!data.length && !error"
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
          Create a new record by clicking the add new record button (<PlusIcon
            class="heroicons"
          />) located in the top left corner of the table.
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
  <ul class="publication" v-for="publication in data" :key="publication.id">
    <li>
      <a
        class="publication-element publication-url"
        :href="publication[doiColumn]"
        target="_blank"
        >{{ publication[labelsColumn] }}</a
      >
    </li>
  </ul>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { MessageBox } from "molgenis-viz";
import { ArrowDownTrayIcon, PlusIcon } from "@heroicons/vue/24/outline";

const props = defineProps<{
  table: string;
  labelsColumn?: string;
  doiColumn?: string;
}>();

interface PublicationProperties {
  id: string;
  title: string;
  doi: string;
}

const error = ref<Error | null>(null);
const data: Record<string, PublicationProperties>[] = ref([]);

async function getPublications() {
  const query = gql`query {
    ${props.table} {
      ${props.labelsColumn || ""}
      ${props.doiColumn}
    }
  }`;
  const response = await request("../api/graphql", query);
  data.value = response[props.table];
}

onMounted(() => {
  getPublications().catch((err) => {
    if (!err.response.errors.length) {
      error.value = err;
    } else {
      error.value = err.response.errors[0].message;
    }
  });
});
</script>

<style lang="scss">
.publication-list-error {
  .heroicons {
    @include setIconSize(24px);
  }
}

.publication-list {
  list-style: none;
  padding: 0;
  margin: 0 auto;
  $border-radius: 8pt;

  .heroicons {
    @include setIconSize(24px);
    path {
      stroke-width: 2;
    }
  }

  .publication {
    display: grid;
    grid-template-columns: 1fr;
    justify-content: center;
    align-items: stretch;
    box-shadow: $box-shadow;
    box-sizing: border-box;
    margin-bottom: 2em;
    padding: 0;
    margin: 0;
    margin-bottom: 2em;
    border-radius: $border-radius;

    .publication-element {
      padding: 0.75em 1.2em;
      margin: 0;
      background-color: $red-050;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .publication-name {
      justify-content: flex-start;
      border-radius: $border-radius 0 0 $border-radius;
    }

    .publication-url {
      justify-content: flex-start;
      background-color: $blue-800;
      color: $gray-050;
      border-radius: 0 $border-radius $border-radius 0;

      &:hover,
      &:focus {
        background-color: $yellow-400;
        color: $blue-800;
      }
    }
  }
}
</style>
