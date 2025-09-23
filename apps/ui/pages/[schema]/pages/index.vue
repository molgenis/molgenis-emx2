<script lang="ts" setup>
import { useFetch } from "#app";
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";

interface Setting {
  key: string;
  value: string;
}

useHead({ title: `Pages - ${schema} - Molgenis` });

type Resp<T> = {
  data: Record<string, T>;
};

const { data } = await useFetch<Resp<Setting[]>>(`/${schema}/graphql`, {
  key: "tables",
  method: "POST",
  body: {
    query: `{_settings{key}}`,
  },
});

const pages = computed<string[]>(() => {
  return data.value?.data?._settings
    ?.filter((setting: Setting) => {
      return setting.key.startsWith("page.");
    })
    .map((setting: Setting) => setting.key.split("page.")[1]) as string[];
});

const crumbs: Record<string, string> = {};
if (schema) {
  crumbs[schema] = `/${schema}`;
}
crumbs["Pages"] = "";
</script>

<template>
  <Container>
    <PageHeader title="Pages" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>
    <ContentBlock v-if="pages" title="Manage pages" description="">
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHeadCell>
              Name
            </TableHeadCell>
            <TableHeadCell>
              Page Options
            </TableHeadCell>
          </TableHeadRow>
        </template>
        <template #body>
          <tr v-for="page in pages">
            <TableBodyCell>
              <NuxtLink :to="`./pages/${page}`">
                {{ page }}
              </NuxtLink>
            </TableBodyCell>
            <TableBodyCell>
              <NuxtLink :to="`./pages/${page}`">
                View
              </NuxtLink>
              <!-- <NuxtLink :to="`./pages/${page}`">
                Edit
              </NuxtLink> -->
            </TableBodyCell>
          </tr>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>
