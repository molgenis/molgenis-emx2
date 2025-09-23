<script lang="ts" setup>
import { useFetch } from "#app";
import { ref, computed, useTemplateRef, onMounted, nextTick } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { generateHtmlPreview, type PageBuilderContent } from "../../../../util/pages";

type Resp<T> = {
  data: Record<string, T>;
};

interface Setting {
  key: string;
  value: string;
}

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";
const page = route.params.page as string;
const htmlContainer = useTemplateRef<HTMLDivElement>("htmlContainer");
const error = ref<string>("");

useHead({ title: `${page} - Pages - ${schema} - Molgenis` });


onMounted(async() => {
  const { data } = await useFetch<Resp<Setting[]>>(`/${schema}/graphql`, {
    key: "tables",
    method: "POST",
    body: {
      query: `{_settings(keys:["page.${page}"]){key value}}`,
    },
  });

  try {
    const setting = data.value?.data._settings?.filter((setting: Setting) => {
      return setting.key === `page.${page}`
    })[0]

    const json = JSON.parse(setting?.value as string) as unknown as PageBuilderContent;
    generateHtmlPreview(json, htmlContainer.value as HTMLDivElement);
  } catch (err) {
    error.value = `Cannot render HTML: ${err}`;
  }
});

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
    <div ref="htmlContainer" />
  </Container>
</template>
