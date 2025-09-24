<script lang="ts" setup>
import { ref} from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { generateHtmlPreview, type PageBuilderContent } from "../../../../../util/pages";

const code = ref({html: '', css: '', javascript: ''});
const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;
useHead({ title: `Edit - ${page} - Pages - ${schema} - Molgenis` });

const crumbs: Record<string, string> = {};
crumbs[schema as string] = `/${schema}`;
crumbs["Pages"] = `/${schema}/pages`;
crumbs[page as string] = `/${schema}/pages/${page}`;
crumbs["Edit"] = "";
</script>

<template>
  <Container>
    <PageHeader :title="`Edit ${page}`" align="left">
      <template #prefix>
        <BreadCrumbs :crumbs="crumbs" align="left" />
      </template>
    </PageHeader>
    <div class="grid grid-cols-2">
      <div class="flex flex-col gap-4">
        <EditorCodeEditor
          lang="html"
          :model-value="code.html"
          @update:model-value="code.html = $event"
        />
        <EditorCodeEditor
          lang="css"
          :model-value="code.css"
          @update:model-value="code.css = $event"
        />
        <EditorCodeEditor
          lang="javascript"
          :model-value="code.javascript"
          @update:model-value="code.javascript = $event"
        />
      </div>
      <div class="bg-gray-100 p-4">
        <h3>Preview</h3>

        {{ code }}
      </div>
    </div>
  </Container>
</template>
