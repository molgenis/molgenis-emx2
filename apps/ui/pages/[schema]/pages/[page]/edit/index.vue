<script lang="ts" setup>
import { ref, onMounted, useTemplateRef, computed, watch } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import type { SideModal } from "#build/components";
import {
  generateHtmlPreview,
  newPageContentObject,
  newPageDate,
  type PageBuilderContent,
  type CssDependency,
  type JavaScriptDependency,
} from "../../../../../util/pages";

interface Setting {
  key: string;
  value: string;
}

interface ModelStatus {
  type: "error" | "success",
  message: string;
}

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;

const isLoading = ref<boolean>(true);
const code = ref<PageBuilderContent>(newPageContentObject("editor"));
const previewElem = useTemplateRef<HTMLDivElement>("preview");
const isSaving = ref<boolean>(false);

const settingsModal = ref<InstanceType<typeof SideModal>>();
const statusModal = ref<InstanceType<typeof SideModal>>();
const statusModalData = ref<ModelStatus>({
  type: "success",
  message: "",
})
const showStatusModal = ref<boolean>(false);

useHead({ title: `Edit - ${page} - Pages - ${schema} - Molgenis` });

function getPageContent () {
  statusModalData.value.message = "";

  $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `{_settings(keys:["page.${page}"]){key value}}`,
    },
  })
  .then((data) => {
    const setting = data?.data?._settings?.filter((setting: Setting) => {
      return setting.key === `page.${page}`;
    })[0];
    code.value = JSON.parse(setting?.value as string) as unknown as PageBuilderContent;
  })
  .catch((err) => {
    statusModalData.value = {
      type: "error",
      message: err
    };
    showStatusModal.value = true;
  })
  .finally(() => isLoading.value = false);
}


onMounted(() => getPageContent());

watch(() => code, () => {
  generateHtmlPreview(code.value, previewElem.value as HTMLDivElement)
}, { deep: true })


async function saveSetting() {
  statusModalData.value.message = "";
  isSaving.value = true;

  code.value.dateModified = newPageDate();

  return $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
     query: `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){status message}}`,
     variables: {
       settings: {
        key: `page.${page}`,
        value: JSON.stringify(code.value)
       }
     }
    }
  })
  .then(response => {
    if (response?.error) {
      throw new Error(response.error[0].message);
    }
    statusModalData.value = {
      type: "success",
      message: "Page saved"
    };
  })
  .catch((err) => {
    statusModalData.value = {
      type: "error",
      message: err
    };
  })
  .finally(() => showStatusModal.value = true);
}

function addCssDependency() {
  code.value.dependencies.css.push({url: ""});
}

function addJsDependency () {
  code.value.dependencies.javascript.push({url: "", defer: false});
}

function updateCssDependency(index: number, value: string) {
  (code.value.dependencies.css[index] as CssDependency).url = value;
}

function updateJsDependency(dependency: JavaScriptDependency,index: number, key: string, value: string) {
  const newDependency = Object.assign(dependency, { [key]: value});
  (code.value.dependencies.css[index] as JavaScriptDependency) = newDependency;
}

function removeCssDependency (index: number) {
  code.value.dependencies.css = code.value.dependencies.css.filter((_,rowNum) => (rowNum !== index));
}

function removeJsDependency(index: number) {
  code.value.dependencies.javascript = code.value.dependencies.javascript.filter((_,rowNum) => rowNum !== index);
}

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
    <div
      class="sticky top-0 w-full flex justify-end items-center bg-content py-2 gap-5 px-7.5 z-10"
    >
      <Button type="outline" size="small" @click="settingsModal?.showModal()">
        Settings
      </Button>
      <Button type="primary" size="small" @click="saveSetting">
        Save Changes
      </Button>
    </div>
    <ContentBlock>
      <div class="grid grid-cols-2 gap-4">
        <div class="flex flex-col gap-y-4">
          <EditorCodeEditor
            lang="html"
            :model-value="code.html"
            @update:model-value="code.html = $event"
          />
          <EditorCodeEditor
            lang="css"
            :modelValue="code.css"
            @update:model-value="code.css = $event"
          />
          <EditorCodeEditor
            lang="javascript"
            :model-value="code.javascript"
            @update:model-value="code.javascript = $event"
          />
        </div>
        <div class="bg-white p-4 max-h-[100vh] overflow-y-scroll">
          <div ref="preview" />
        </div>
      </div>
    </ContentBlock>
  </Container>
  <SideModal
    ref="settingsModal"
    :slide-in-right="true"
    :full-screen="false"
    button-alignment="right"
    :include-footer="true"
  >
    <ContentBlockModal title="Settings" class="text-title-contrast">
      <form @submit.prevent>
        <h3 class="text-heading-4xl text-title-contrast font-display mb-2.5">
          Manage dependencies
        </h3>
        <p class="mb-2.5">Removing dependencies will require a page refresh.</p>
        <fieldset class="my-5">
          <legend class="text-title-contrast font-bold mb-2.5">
            CSS Dependencies
          </legend>
          <div
            v-if="code.dependencies.css"
            v-for="(dependency, index) in code.dependencies.css"
            class="flex justify-start items-center gap-5 mb-2.5"
          >
            <div class="w-full">
              <label
                class="sr-only"
                :for="`form-editor-settings-dependency-css-${index}`"
              >
                Enter URL to dependency
              </label>
              <InputString
                :id="`form-editor-settings-dependency-css-${index}`"
                placeholder="https://path/to/css"
                :model-value="dependency.url"
                @update:model-value="updateCssDependency(index, ($event as string))"
              />
            </div>
            <div>
              <Button
                type="inline"
                :icon-only="true"
                label="Delete"
                icon="Trash"
                size="small"
                class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                @click="removeCssDependency(index)"
              />
            </div>
          </div>
          <Button
            type="text"
            icon="Plus"
            @click="addCssDependency"
            size="small"
            class="my-2.5"
          >
            Add dependency
          </Button>
        </fieldset>
        <fieldset class="my-5">
          <legend class="text-title-contrast font-bold mb-2.5">
            JavaScript dependencies
          </legend>
          <div
            v-if="code.dependencies.javascript"
            v-for="(dependency, index) in code.dependencies.javascript"
            class="flex justify-between items-center gap-5 mb-2.5"
          >
            <div>
              <label
                class="sr-only"
                :for="`form-editor-settings-dependency-js-${index}`"
              >
                Enter dependency URL
              </label>
              <InputString
                :id="`form-editor-settings-dependency-js-${index}`"
                placeholder="https://path/to/js"
                :model-value="dependency.url"
                @update:model-value="updateJsDependency(dependency, index, 'url', ($event as string))"
              />
            </div>
            <div>
              <label :for="`form-editor-settings-dependency-js-${index}-defer`">
                Defer?
              </label>
              <InputBoolean
                :id="`form-editor-settings-dependency-js-${index}-defer`"
                class="[&_svg]:mt-0"
                :model-value="dependency.defer"
                true-label="Yes"
                false-label="No"
                :show-clear-button="false"
                align="horizontal"
              />
            </div>
            <div>
              <Button
                type="inline"
                :icon-only="true"
                icon="Trash"
                label="Delete"
                size="small"
                class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                @click="removeJsDependency(index)"
              />
            </div>
          </div>
          <Button
            type="text"
            icon="Plus"
            @click="addJsDependency"
            size="small"
            class="my-2.5"
          >
            Add dependency
          </Button>
        </fieldset>
      </form>
    </ContentBlockModal>
  </SideModal>
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
