<script setup lang="ts">
import { useHead, useRuntimeConfig, navigateTo, useFetch } from "#app";
import { definePageMeta } from "#imports";
import LayoutsLandingPage from "../components/layouts/LandingPage.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import { landingPageRedirect } from "../utils/landingPageRedirect";
import ContentBlock from "../../../tailwind-components/app/components/content/ContentBlock.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";
import Table from "../../../tailwind-components/app/components/Table.vue";
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import { ref, computed } from "vue";
import { toFormData } from "../../../metadata-utils/src/toFormData.ts";
import type { columnValue } from "../../../metadata-utils/src/types.ts";

definePageMeta({
  middleware: [
    function (to) {
      // if paper extracting is not enabled and setup, redirect to the main catalogue page
      if (
        !useRuntimeConfig().public.isPaperExtractingEnabled ||
        !useRuntimeConfig().public.paperExtractingSchema
      ) {
        const target = landingPageRedirect(
          to.query,
          useRuntimeConfig().public.cohortOnly
        );
        if (target) {
          return navigateTo(target, {
            replace: true,
          });
        }
      }
    },
  ],
});

const JOB_START_STATUS = "CREATED";
const config = useRuntimeConfig();
const paperExtractingSchema = config.public.paperExtractingSchema as string;

const pageTitle = "Paper Extracting";
const pageDescription =
  "This page allows you to extract data from research papers and add it to the catalogue.";

useHead(() => ({
  title: pageTitle,
  meta: [
    {
      name: "description",
      content: pageDescription,
    },
  ],
}));

const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const isDragging = ref(false);
const errorMessage = ref("");
const isSubmitting = ref(false);

const { data: myJobs, status: jobsStatus } = await useFetch(
  `/${paperExtractingSchema}/graphql`,
  {
    method: "POST",
    server: false,

    body: JSON.stringify({
      query: `query { Jobs {
     id 
     input { id, size, filename, extension, url } 
     status { label mg_draft mg_insertedBy mg_insertedOn mg_updatedBy mg_updatedOn } 
     output { id, size, filename, extension, url } 
     mg_draft mg_insertedBy mg_insertedOn mg_updatedBy mg_updatedOn
    } }`,
    }),
    headers: {
      "Content-Type": "application/json",
    },
  }
);

const fileSize = computed(() => {
  if (!selectedFile.value) return "";
  const kb = selectedFile.value.size / 1024;
  return kb < 1024 ? `${kb.toFixed(0)} KB` : `${(kb / 1024).toFixed(1)} MB`;
});

function isPdf(file: File) {
  return (
    file.type === "application/pdf" || file.name.toLowerCase().endsWith(".pdf")
  );
}

function setFile(file: File | null | undefined) {
  errorMessage.value = "";
  if (!file) return;
  if (!isPdf(file)) {
    errorMessage.value = `"${file.name}" is not a PDF file. Please select a PDF.`;
    return;
  }
  selectedFile.value = file;
}

function openFilePicker() {
  fileInput.value?.click();
}

function onFileInput(event: Event) {
  const input = event.target as HTMLInputElement;
  setFile(input.files?.item(0));
  // reset so selecting the same file again still triggers the input event
  input.value = "";
}

function onDrop(event: DragEvent) {
  isDragging.value = false;
  setFile(event.dataTransfer?.files?.item(0));
}

function removeFile() {
  selectedFile.value = null;
  errorMessage.value = "";
}

async function onSubmit() {
  if (!selectedFile.value) {
    errorMessage.value = "Please add a PDF file before submitting.";
    return;
  }
  isSubmitting.value = true;
  try {
    const query = `mutation insert($value:[JobsInput]){insert(Jobs:$value){message}}`;
    const formValues: Record<string, columnValue> = {
      status: { label: JOB_START_STATUS },
      // @ts-ignore
      input: selectedFile.value,
    };
    const formData = toFormData(formValues);
    formData.append("query", query);
    const res = await $fetch(`/${paperExtractingSchema}/graphql`, {
      method: "POST",
      body: formData,
    });
    return res;
  } catch (error) {
    console.error("Error submitting job:", error);
    errorMessage.value = "Failed to submit the job. Please try again.";
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<template>
  <LayoutsLandingPage>
    <PageHeader
      :title="pageTitle"
      :description="pageDescription"
      :truncate="false"
    >
      <template #suffix>
        <div
          class="relative justify-center flex flex-col md:flex-row text-title"
        ></div>
      </template>
    </PageHeader>
    <div class="flex flex-col gap-10">
      <ContentBlock
        v-if="myJobs?.data?.Jobs?.length"
        title="My jobs"
        description="Papers you have submitted for extraction."
      >
        <Table>
          <template #head>
            <TableHeadRow>
              <TableHead>Id</TableHead>
              <TableHead>File name</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Result</TableHead>
            </TableHeadRow>
          </template>
          <template #body>
            <TableRow v-for="job in myJobs.data.Jobs" :key="job.id" disabled>
              <TableCell>{{ job.id }}</TableCell>
              <TableCell>{{ job.input?.filename }}</TableCell>
              <TableCell>{{ job.status?.label }}</TableCell>
              <TableCell>
                <a
                  v-if="job.output?.url"
                  :href="job.output.url"
                  :download="job.output.filename"
                  class="text-link underline"
                >
                  {{ job.output.filename }}
                </a>
                <span v-else>-</span>
              </TableCell>
            </TableRow>
          </template>
        </Table>
      </ContentBlock>
      <ContentBlock
        v-if="jobsStatus !== 'pending'"
        title="Upload paper"
        description="Add a research paper in PDF format to start extracting its data."
      >
        <form class="flex flex-col gap-5" @submit.prevent="onSubmit">
          <div
            role="button"
            tabindex="0"
            aria-label="Add a PDF file: drop it here or press to browse"
            class="flex flex-col items-center justify-center gap-3 p-12.5 border-2 border-dashed rounded-alt cursor-pointer text-center duration-default ease-in-out transition-colors [&>*]:pointer-events-none"
            :class="{
              'border-input-focused bg-hover': isDragging,
              'border-valid bg-valid text-valid': selectedFile && !isDragging,
              'border-invalid': errorMessage && !selectedFile && !isDragging,
              'border-input hover:border-input-hover':
                !isDragging && !selectedFile && !errorMessage,
            }"
            @click="openFilePicker"
            @keydown.enter.prevent="openFilePicker"
            @keydown.space.prevent="openFilePicker"
            @dragenter.prevent="isDragging = true"
            @dragover.prevent="isDragging = true"
            @dragleave.prevent="isDragging = false"
            @drop.prevent="onDrop"
          >
            <BaseIcon
              :name="selectedFile ? 'check' : 'upload-file'"
              :width="48"
            />
            <template v-if="selectedFile">
              <p class="font-bold">{{ selectedFile.name }}</p>
              <p class="text-body-sm">{{ fileSize }} · click to replace</p>
            </template>
            <template v-else>
              <p class="font-bold">
                {{
                  isDragging ? "Drop your PDF here" : "Drag and drop a PDF here"
                }}
              </p>
              <p class="text-body-sm">
                or <span class="text-link underline">browse your files</span>
              </p>
            </template>
            <input
              id="paper-extracting-file-input"
              ref="fileInput"
              class="sr-only"
              type="file"
              accept="application/pdf,.pdf"
              @input="onFileInput"
            />
          </div>

          <Message
            v-if="selectedFile"
            id="paper-extracting-file-added"
            valid
            aria-live="polite"
          >
            <span class="ml-3 grow">File added: {{ selectedFile.name }}</span>
            <Button
              type="text"
              size="tiny"
              icon="trash"
              label="Remove"
              @click.prevent="removeFile"
            />
          </Message>
          <Message
            v-else-if="errorMessage"
            id="paper-extracting-file-error"
            invalid
            aria-live="assertive"
          >
            <span class="ml-3">{{ errorMessage }}</span>
          </Message>

          <div class="flex justify-end">
            <Button
              label="Start extracting"
              size="large"
              :disabled="!selectedFile || isSubmitting"
            />
          </div>
        </form>
      </ContentBlock>
    </div>
  </LayoutsLandingPage>
</template>
