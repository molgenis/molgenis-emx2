<script lang="ts" setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
// import { type PageBuilderContent } from "../../../../util/pages";
import type { DeveloperPage } from "~/util/PageTypes";

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

const developerPage = ref<DeveloperPage>();

useHead({ title: `${page} - Pages - ${schema} - Molgenis` });

async function getPageContent() {
  const response = await $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: `query getDeveloperPage($filter:DeveloperPageFilter) {
        DeveloperPage(filter:$filter) {
          name
          description
          html
          css
          javascript
          dependencies {
            mg_tableclass
            name
            url
            defer
            async
            fetchPriority {
              name
            }
          }
          enableBaseStyles
          enableButtonStyles
          enableFullScreen
        }
      }`,
      variables: { filter: { name: { equals: page } } },
    },
  }).catch((err) => {
    error.value = `Cannot render HTML: ${err}`;
  });

  if (response) {
    developerPage.value = response.data.DeveloperPage[0];
  }
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
    {{ developerPage }}
  </Container>
  <!-- <EditorHtmlPreview :code="code" v-if="code" /> -->
</template>
