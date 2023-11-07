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
        <p>Unable to retrieve available files.</p>
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
import { ArrowDownTrayIcon } from "@heroicons/vue/24/outline";

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
  $border-radius: 8pt;

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
      padding: 0.8em 1.2em;
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
      background-color: $blue-050;
      color: $blue-800;
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
