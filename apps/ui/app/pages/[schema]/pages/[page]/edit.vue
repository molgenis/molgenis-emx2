<script lang="ts" setup>
import { ref, watch, computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

import type { INotificationType } from "../../../../../../tailwind-components/types/types";
import Container from "../../../../../../tailwind-components/app/components/Container.vue";
import ContentBlockModal from "../../../../../../tailwind-components/app/components/content/ContentBlockModal.vue";
import SideModal from "../../../../../../tailwind-components/app/components/SideModal.vue";
import PageHeader from "../../../../../../tailwind-components/app/components/PageHeader.vue";
import BreadCrumbs from "../../../../../../tailwind-components/app/components/BreadCrumbs.vue";
import Button from "../../../../../../tailwind-components/app/components/Button.vue";
import Message from "../../../../../../tailwind-components/app/components/Message.vue";
import CodeEditor from "../../../../../../tailwind-components/app/components/editor/CodeEditor.vue";
import HtmlPreview from "../../../../../../tailwind-components/app/components/editor/HtmlPreview.vue";
import {
  type DeveloperPage,
  getPage,
  newDeveloperPage,
} from "../../../../../../tailwind-components/app/utils/Pages";

import { useSession } from "../../../../../../tailwind-components/app/composables/useSession";
const { isAdmin } = await useSession();

interface ModelStatus {
  type: INotificationType;
  message: string;
}

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;

useHead({ title: `Edit - ${page} - Pages - ${schema} - Molgenis` });

const pageData = ref<DeveloperPage>(newDeveloperPage());
const originalPageData = ref<DeveloperPage>(newDeveloperPage());

pageData.value = await getPage(schema as string, page);
originalPageData.value = { ...pageData.value };

const hasUnsavedHtml = ref<boolean>(false);
const hasUnsavedCss = ref<boolean>(false);
const hasUnsavedJs = ref<boolean>(false);

const isSaving = ref<boolean>(false);
const statusModal = ref<InstanceType<typeof SideModal>>();
const statusModalData = ref<ModelStatus>({
  type: "info",
  message: "",
});
const showStatusModal = ref<boolean>(false);

async function saveSetting() {
  statusModalData.value.message = "";
  isSaving.value = true;

  const response = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `mutation save($page:[DeveloperPageInput]){save(DeveloperPage:$page){status message}}`,
      variables: { page: pageData.value },
    },
  }).catch((err) => {
    statusModalData.value = {
      type: "error",
      message: err.error ? err.error[0].message : err,
    };
    console.log(statusModalData.value.message);
  });

  if (response.data?.save) {
    statusModalData.value = {
      type: "success",
      message: `Updated "${page}"`,
    };
  }
  showStatusModal.value = true;
}

const enableSaveButton = computed<boolean>(
  () => hasUnsavedHtml.value || hasUnsavedCss.value || hasUnsavedJs.value
);

function hasUnsavedChanges(
  type: string,
  newValue: string,
  oldValue: string
): boolean {
  const newValueStr = JSON.stringify(newValue);
  const oldValueStr = JSON.stringify(oldValue);
  const originalValueStr = JSON.stringify(
    originalPageData.value[type as keyof DeveloperPage]
  );
  return newValueStr !== oldValueStr && newValueStr !== originalValueStr;
}

watch(
  () => pageData.value.html,
  (newValue, oldValue) => {
    hasUnsavedHtml.value = hasUnsavedChanges(
      "html",
      newValue as string,
      oldValue as string
    );
  }
);

watch(
  () => pageData.value.css,
  (newValue, oldValue) => {
    hasUnsavedCss.value = hasUnsavedChanges(
      "css",
      newValue as string,
      oldValue as string
    );
  }
);

watch(
  () => pageData.value.javascript,
  (newValue, oldValue) => {
    hasUnsavedJs.value = hasUnsavedChanges(
      "javascript",
      newValue as string,
      oldValue as string
    );
  }
);

const crumbs: Record<string, string> = {};
crumbs[schema as string] = `/${schema}`;
crumbs["Pages"] = `/${schema}/pages`;
crumbs[page as string] = `/${schema}/pages/${page}`;
crumbs["Edit"] = "";
</script>

<template>
  <Container class="relative">
    <PageHeader :title="`Edit ${page}`" align="left">
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" align="left" />
      </template>
    </PageHeader>
    <div v-if="!isAdmin">
      <Message id="page-editor-error" :invalid="true">
        <p>Please sign in as "admin" to modify page content.</p>
      </Message>
    </div>
    <template v-else>
      <div
        class="sticky top-0 w-full flex justify-end items-center bg-navigation py-2 gap-5 px-7.5 z-10 mb-5"
      >
        <NuxtLink
          :to="`/${schema}/pages/${page}`"
          class="font-display tracking-widest uppercase text-button-text text-heading-lg hover:underline"
        >
          View page
        </NuxtLink>
        <Button
          type="primary"
          size="small"
          :disabled="!enableSaveButton"
          :class="{
            'animate-pulse': enableSaveButton,
            'cursor-not-allowed': !enableSaveButton,
          }"
          @click="saveSetting"
        >
          Save Changes
        </Button>
      </div>
      <div class="grid grid-cols-2 gap-7.5 max-h-lvh">
        <CodeEditor
          lang="html"
          :model-value="pageData.html"
          @update:model-value="pageData.html = $event"
        />
        <div
          class="bg-white border border-input rounded p-7.5 overflow-y-scroll max-h-80"
        >
          <HtmlPreview :content="pageData" />
        </div>
        <CodeEditor
          lang="css"
          :modelValue="pageData.css"
          @update:model-value="pageData.css = $event"
        />
        <CodeEditor
          lang="javascript"
          :model-value="pageData.javascript"
          @update:model-value="pageData.javascript = $event"
        />
      </div>
    </template>
  </Container>
  <SideModal
    ref="statusModal"
    :type="statusModalData.type"
    :show="showStatusModal"
    :slide-in-right="true"
    :full-screen="false"
    :include-footer="false"
    @close="showStatusModal = false"
  >
    <ContentBlockModal
      :title="statusModalData.type.toUpperCase()"
      :class="{
        '!bg-invalid text-invalid': statusModalData.type === 'error',
        '!bg-valid text-valid': statusModalData.type === 'success',
      }"
    >
      <p>{{ statusModalData.message }}</p>
    </ContentBlockModal>
  </SideModal>
</template>
