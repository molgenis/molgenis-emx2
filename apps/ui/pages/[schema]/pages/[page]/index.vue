<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { type PageBuilderContent } from "../../../../util/pages";

interface Setting {
  key: string;
  value: string;
}

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;
const error = ref<string>("");

const code = ref<PageBuilderContent>();

useHead({ title: `${page} - Pages - ${schema} - Molgenis` });

function getPageContent () {
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
    error.value = `Cannot render HTML: ${err}`;
  })
}

onMounted(() => getPageContent());

const crumbs: Record<string, string> = {};
crumbs[schema as string] = `/${schema}`;
crumbs["Pages"] = `/${schema}/pages`;
crumbs[page as string] = "";
</script>

<template>
  <Container>
    <bread-crumbs align="left" :crumbs="crumbs" class="my-5" />
    <Message
      :id="`page-${page?.replace(' ', '-')}`"
      :invalid="true"
      v-if="error"
    >
      {{ error }}
    </Message>
  </Container>
  <EditorHtmlPreview :code="code" v-if="code" />
</template>
