<template>
  <Page id="page-documents">
    <PageHeader
      class="ern-header"
      title="ERN CRANIO"
      subtitle="Documents"
      imageSrc="banner-diagnoses.jpg"
    />
    <Breadcrumbs />
    <PageSection
      id="section-documents"
      aria-labelledby="section-documents-title"
      :verticalPadding="2"
      :horizontalPadding="0"
    >
      <h2 id="section-documents-title">Download Documents</h2>
      <p>Download additional information about the CRANIO Registry.</p>
      <MessageBox type="error" v-if="error">
        <p>{{ error }}</p>
      </MessageBox>
      <MessageBox type="error" v-else-if="!files && !error">
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
      <ul class="document-list" v-else>
        <li class="file" v-for="file in files" :key="file.path.id">
          <p class="file-element file-name">{{ file.name }}</p>
          <p class="file-element file-format">
            {{ file.path.extension }}
          </p>
          <p class="file-element file-size">
            {{ Math.round((file.path.size / 1000) * 100) / 100 }} KB
          </p>
          <a class="file-element file-url" :href="file.path.url">
            <span class="visually-hidden">Download {{ file.name }}</span>
            <ArrowDownTrayIcon class="heroicons" />
          </a>
        </li>
      </ul>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import gql from "graphql-tag";
import { request } from "graphql-request";
import { Page, PageHeader, PageSection, MessageBox } from "molgenis-viz";
import Breadcrumbs from "../components/breadcrumbs.vue";
import { ArrowDownTrayIcon, PlusIcon } from "@heroicons/vue/24/outline";

let error = ref(null);
let files = ref([]);

async function getFiles() {
  const query = gql`
    {
      Files {
        name
        path {
          id
          size
          extension
          url
        }
      }
    }
  `;
  const response = await request("../api/graphql", query);
  files.value = response.Files;
}

onMounted(() => {
  getFiles().catch((err) => {
    error.value = true;
    throw new Error("Unable to retrieve files", { cause: err });
  });
});
</script>

<style lang="scss">
.document-list {
  list-style: none;
  padding: 0;
  margin: 0 auto;
  $border-radius: 16pt;

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
      background-color: $ern-cranio-primary;
      color: $gray-000;
      border-radius: 0 $border-radius $border-radius 0;

      .heroicons {
        $size: 18pt;
        width: $size;
        height: $size;
        path {
          stroke-width: 2;
        }
      }

      &:hover,
      &:focus {
        background-color: $blue-800;
        color: $blue-050;
      }
    }
  }
}
</style>
