<template>
  <MessageBox type="error" v-if="error" class="file-list-error">
    <p><strong>Unable to retrieve files</strong></p>
    <p>{{ error }}</p>
  </MessageBox>
  <MessageBox type="error" v-else-if="!files && !error" class="file-list-error">
    <div class="p-2">
      <p>
        No files are available for download. To import files, follow the
        steps outlines below.
      </p>
      <ol>
        <li>Navigate to the <a href="../tables/#/Files">Files table</a></li>
        <li>
          Create a new record by clicking the add new record button
          (<PlusIcon class="heroicons" />) located in the top left corner of
          the table.
        </li>
        <li>
          In the form that appears, enter as much information about the file
          as necessary. Make sure all required fields are completed.
        </li>
        <li>
          To import your file, see the input field for the column "path".
          This is used for selecting and importing a file into MOLGENIS.
          Click the "browse" button to find and select your file. Click the
          import button to import your file.
        </li>
        <li>
          When you have finished, click "save". Your file will be added as a
          new record in the table.
        </li>
      </ol>
      <p>Repeat the process for each file.</p>
    </div>
  </MessageBox>
  <ul class="file-list" v-else>
    <li class="file" v-for="file in files" :key="file[path].id">
      <p class="file-element file-name">{{ file[filename] }}</p>
      <p class="file-element file-format">
        {{ file[path].extension }}
      </p>
      <p class="file-element file-size">
        {{ Math.round((file[path].size / 1000) * 100) / 100 }} KB
      </p>
      <a class="file-element file-url" :href="file[path].url">
        <span class="visually-hidden">Download {{ file[filename] }}</span>
        <ArrowDownTrayIcon class="heroicons" />
      </a>
    </li>
  </ul>
</template>

<script setup>
import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { MessageBox } from "molgenis-viz";
import { ArrowDownTrayIcon, PlusIcon } from "@heroicons/vue/24/outline";

let error = ref(null);
let files = ref([]);

const props = defineProps({
  
  // name of the table in the schema that contains the file metadata
  table: {
    type: String,
    required: true
  },
  
  // name of the column that contains the file names to display
  filename: {
    type :String,
    required: true
  },
  
  // name of the column that contains the location of the file
  path: {
    type: String,
    required: true
  }
});

async function getFiles() {
  const query = gql`{
    ${props.table} {
      ${ props.filename }
      ${ props.path } {
        id
        size
        extension
        url
      }
    }
  }`;
  const response = await request("../api/graphql", query);
  files.value = response[props.table];
}

onMounted(() => {
  getFiles().catch(err => {
    if (!err.response.errors.length) {
      error.value = err;
    } else {
      error.value = err.response.errors[0].message;
    }
  });
});
</script>

<style lang="scss">
.file-list-error {
  .heroicons {
    @include setIconSize(24px);
  }
}

.file-list {
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

  .file {
    display: grid;
    grid-template-columns: 1fr repeat(2, 0.5fr) 0.2fr;
    justify-content: center;
    align-items: stretch;
    box-shadow: $box-shadow;
    box-sizing: border-box;
    margin-bottom: 2em;
    padding: 0;
    margin: 0;
    margin-bottom: 2em;
    border-radius: $border-radius;

    .file-element {
      padding: 0.75em 1.2em;
      margin: 0;
      background-color: $red-050;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .file-name {
      justify-content: flex-start;
      border-radius: $border-radius 0 0 $border-radius;
    }

    .file-url {
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
