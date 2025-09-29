<script lang="ts" setup>
import { ref, onMounted, useTemplateRef, computed, watch } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { useForm } from "#imports";
import type { ColumnType } from "../../../../../../metadata-utils/src/types";
import {
  generateHtmlPreview,
  type PageBuilderContent,
  newPageContentObject

} from "../../../../../util/pages";


interface Setting {
  key: string;
  value: string;
}

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;
const cleanPageName = computed<string>(() => page?.replace(' ', '-').toLowerCase());

useHead({ title: `Edit - ${page} - Pages - ${schema} - Molgenis` });

const loading = ref<boolean>(true);
const code = ref<PageBuilderContent>(newPageContentObject("advanced"));
const error = ref<string>("");
const previewElem = useTemplateRef<HTMLDivElement>("preview");
const isSaving = ref<boolean>(false);
const saveError = ref<string>("");
const saveSuccessStatus = ref<boolean | null>();

async function getPageContent () {
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
    error.value = `Cannot render HTML:\n\n${err}`
  })
  .finally(() => loading.value = false);
}


onMounted(() => getPageContent());

watch(() => code, () => {
  generateHtmlPreview(code.value, previewElem.value as HTMLDivElement)
}, { deep: true })


async function saveSetting() {
  isSaving.value = true;
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
    saveSuccessStatus.value = true;
  })
  .catch((err) => saveError.value = err)
  .finally(() => isSaving.value = false)
}

const model = ref({});
const metadata = ref({
  id: "page-dependencies",
  label: 'External Dependencies',
  tableType: "FORM",
  columns: [
    {
      columnType: 'HYPERLINK_ARRAY' as ColumnType,
      id: 'css',
      label: 'CSS dependencies'
    },
  ]
});

const test = ref();

const { errorMap, onUpdateColumn, onBlurColumn } = useForm(metadata, code.value.dependencies);

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
      <Button type="primary" size="small" @click="saveSetting">
        Save Changes
      </Button>
    </div>
    <ContentBlock>
      <Message
        :id="`page-${cleanPageName}`"
        :invalid="true"
        v-if="!loading && error"
      >
        {{ error }}
      </Message>
      <template v-else>
        <SideModal
          v-if="!isSaving && saveError"
          :show="true"
          :slide-in-right="true"
          type="error"
          :full-screen="false"
        >
          <template>
            <p><strong>Unable to save schema</strong></p>
          </template>
        </SideModal>
        <Message
          :valid="true"
          :id="`page-${cleanPageName}`"
          v-if="!isSaving && saveSuccessStatus"
        >
          <p>Saved {{ page }} content</p>
        </Message>
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
            <FormFields
              id="dependencies"
              v-model="model"
              :columns="metadata.columns"
              :error-map="errorMap"
              @update="onUpdateColumn"
              @blur="onBlurColumn"
            />
            {{ test }}
            <EditorDependencyInput
              :model-value="test"
              @update:model-value="console.log(model)"
            />
          </div>
          <div class="bg-white p-4">
            <div ref="preview" />
          </div>
        </div>
      </template>
    </ContentBlock>
  </Container>
</template>
