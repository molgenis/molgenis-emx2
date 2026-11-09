<script setup lang="ts">
import {
  useHead,
  useRoute,
  useRuntimeConfig,
  navigateTo,
  useFetch,
} from "#app";
import LayoutsLandingPage from "../components/layouts/LandingPage.vue";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import { landingPageRedirect } from "../utils/landingPageRedirect";
import { openReAuthenticationWindow } from "../../../tailwind-components/app/utils/openReAuthenticationWindow";
import {
  usePaperExtractingAccess,
  usePaperExtractingSettings,
} from "../composables/usePaperExtractingAccess";
import ContentBlock from "../../../tailwind-components/app/components/content/ContentBlock.vue";
import Message from "../../../tailwind-components/app/components/Message.vue";
import BaseIcon from "../../../tailwind-components/app/components/BaseIcon.vue";
import Table from "../../../tailwind-components/app/components/Table.vue";
import TableCell from "../../../tailwind-components/app/components/TableCell.vue";
import TableHead from "../../../tailwind-components/app/components/TableHead.vue";
import TableHeadRow from "../../../tailwind-components/app/components/TableHeadRow.vue";
import TableRow from "../../../tailwind-components/app/components/TableRow.vue";
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { toFormData } from "../../../metadata-utils/src/toFormData.ts";
import type { columnValue } from "../../../metadata-utils/src/types.ts";

const JOB_START_STATUS = "CREATED";
const route = useRoute();
const config = useRuntimeConfig();

// paper extracting is configured in the catalogue schema settings, if it is
// not enabled and setup, redirect to the main catalogue page
const paperExtractingSettings = await usePaperExtractingSettings();
if (!paperExtractingSettings.enabled) {
  await navigateTo(
    landingPageRedirect(route.query, config.public.cohortOnly as boolean) ??
      "/",
    { replace: true }
  );
}
const paperExtractingSchema = paperExtractingSettings.schema;

const pageTitle = "Paper Extracting";
const pageDescription =
  "Extract data from research papers and add it to the catalogue.";

useHead(() => ({
  title: pageTitle,
  meta: [
    {
      name: "description",
      content: pageDescription,
    },
  ],
}));

const selectedFile = ref<File | null>(null);
const isDragging = ref(false);
const errorMessage = ref("");
const isSubmitting = ref(false);

// everyone can see the page, but only admins, schema owners and users with a
// 'Submit_' role can see their jobs and submit papers
const { isSignedIn, hasAccess } = await usePaperExtractingAccess(
  paperExtractingSchema
);

const LOGIN_POPUP_WIDTH = 600;
const LOGIN_POPUP_HEIGHT = 700;
let loginMessageHandler: ((event: MessageEvent) => void) | undefined;

function stopListeningForLogin() {
  if (loginMessageHandler) {
    window.removeEventListener("message", loginMessageHandler);
    loginMessageHandler = undefined;
  }
}

// the login page lives in the ui app, so sign in using a popup that reports
// back when it is done, the user then stays on this page
function signIn() {
  stopListeningForLogin();
  const popup = openReAuthenticationWindow(
    window,
    "/apps/ui/login?reauthenticate=true",
    LOGIN_POPUP_WIDTH,
    LOGIN_POPUP_HEIGHT
  );
  if (!popup) {
    errorMessage.value =
      "The sign in window was blocked. Please allow pop-ups for this site and try again.";
    return;
  }
  errorMessage.value = "";
  loginMessageHandler = (event: MessageEvent) => {
    if (
      event.origin !== window.location.origin ||
      event.source !== popup ||
      event.data?.status !== "reAuthenticated"
    ) {
      return;
    }
    stopListeningForLogin();
    // access is determined during page setup, so reload to pick up the session
    window.location.reload();
  };
  window.addEventListener("message", loginMessageHandler);
}

const {
  data: myJobs,
  status: jobsStatus,
  refresh: refreshJobs,
} = await useFetch(`/${paperExtractingSchema}/graphql`, {
  method: "POST",
  server: false,
  // only fetch the jobs when the user is allowed to use paper extracting
  immediate: hasAccess.value,

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
});

// reload the jobs every 30 seconds so status changes from the backend show up
const JOBS_REFRESH_INTERVAL_MS = 30_000;
let jobsRefreshTimer: ReturnType<typeof setInterval> | undefined;

onMounted(() => {
  if (!hasAccess.value) return;
  jobsRefreshTimer = setInterval(() => {
    // skip when the previous (or a submit triggered) refresh is still running
    if (jobsStatus.value !== "pending") {
      refreshJobs();
    }
  }, JOBS_REFRESH_INTERVAL_MS);
});

onBeforeUnmount(() => {
  clearInterval(jobsRefreshTimer);
  stopListeningForLogin();
});

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
  if (!hasAccess.value) return;
  errorMessage.value = "";
  if (!file) return;
  if (!isPdf(file)) {
    errorMessage.value = `"${file.name}" is not a PDF file. Please select a PDF.`;
    return;
  }
  selectedFile.value = file;
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
  if (!hasAccess.value) return;
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
    await $fetch(`/${paperExtractingSchema}/graphql`, {
      method: "POST",
      body: formData,
    });
    selectedFile.value = null;
    await refreshJobs();
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
        title="Submitted papers"
        description="Papers you have submitted for extraction."
      >
        <!-- {{ myJobs.data.Jobs }} -->
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
      <Message v-if="!hasAccess" id="paper-extracting-no-access">
        <span class="ml-3">
          <template v-if="isSignedIn">
            Your account is not allowed to use paper extracting yet. Please ask
            the project administrator to give you access.
          </template>
          <template v-else>
            Please sign in and ask the project administrator to allow your
            account to use paper extracting.
          </template>
        </span>
        <Button
          v-if="!isSignedIn"
          class="ml-3 shrink-0 border bordert-theme"
          size="small"
          type="secondary"
          label="Sign in"
          @click="signIn"
        />
      </Message>
      <ContentBlock
        v-if="!hasAccess || myJobs || jobsStatus !== 'pending'"
        title="Upload paper"
        description="Add a research paper in PDF format to start extracting its data."
      >
        <form
          class="flex flex-col gap-5 w-full max-w-xl"
          @submit.prevent="onSubmit"
        >
          <fieldset :disabled="!hasAccess" class="contents">
            <label
              for="paper-extracting-file-input"
              class="flex flex-col items-center justify-center gap-3 p-12.5 border-2 border-dashed rounded-alt text-center duration-default ease-in-out transition-colors focus-within:ring-2 focus-within:ring-offset-2 [&>*]:pointer-events-none"
              :class="{
                'cursor-pointer': hasAccess,
                'border-disabled bg-disabled text-disabled cursor-not-allowed':
                  !hasAccess,
                'border-input-focused bg-hover': isDragging,
                'border-valid bg-valid text-valid': selectedFile && !isDragging,
                'border-invalid': errorMessage && !selectedFile && !isDragging,
                'border-input hover:border-input-hover':
                  hasAccess && !isDragging && !selectedFile && !errorMessage,
              }"
              @dragenter.prevent="isDragging = hasAccess"
              @dragover.prevent="isDragging = hasAccess"
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
                    isDragging
                      ? "Drop your PDF here"
                      : "Drag and drop a PDF here"
                  }}
                </p>
                <p class="text-body-sm">
                  or <span class="text-link underline">browse your files</span>
                </p>
              </template>
              <input
                id="paper-extracting-file-input"
                class="sr-only"
                type="file"
                accept="application/pdf,.pdf"
                @input="onFileInput"
              />
            </label>

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

            <div class="flex justify-start">
              <Button
                label="Start extracting"
                size="large"
                :disabled="!hasAccess || !selectedFile || isSubmitting"
              />
            </div>
          </fieldset>
        </form>
      </ContentBlock>
    </div>
  </LayoutsLandingPage>
</template>
