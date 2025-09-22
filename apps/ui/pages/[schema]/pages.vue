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

interface Pages {
  pageBuilder: string[];
  advancedBuilder: string[];
}

useHead({ title: `Pages - ${schema}  - Molgenis` });

type Resp<T> = {
  data: Record<string, T>;
};

const { data } = await useFetch<Resp<Setting[]>>(`/${schema}/graphql`, {
  key: "tables",
  method: "POST",
  body: {
    query: `{_settings{key,value}}`,
  },
});

const pages = computed<Pages>(() => {
  return (data.value?.data?._settings?.filter((setting: Setting) => {
    return setting.key.startsWith("page.");
  }) as Setting[]).reduce(
    (acc: Pages, setting: Setting) => {
      console.log(setting.value.match("__pageBuilder__"));
      if (setting.value.match("__pageBuilder__")) {
        acc["pageBuilder"].push(setting.key);
      } else {
        acc["advancedBuilder"].push(setting.key);
      }
      return acc as Pages;
    },
    { pageBuilder: [], advancedBuilder: [] } as Pages
  );
});

const crumbs: Record<string, string> = {};
if (schema) {
  crumbs[schema] = `/${schema}`;
}
crumbs["Pages"] = "";
</script>

<template>
  {{ pages }}
  <Container>
    <PageHeader title="Pages" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>
    <ContentBlock
      v-if="pages.pageBuilder"
      title="Page Builder"
      description="Pages created with the web page builder interface"
    >
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHeadCell>Page Name</TableHeadCell>
            <TableHeadCell>Date Created</TableHeadCell>
            <TableHeadCell>Date Modified</TableHeadCell>
            <TableHeadCell>Page Options</TableHeadCell>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow
            v-for="page in pages.pageBuilder.map((page: string) => page.split('page.')[1])"
          >
            <TableBodyCell>
              {{ page }}
            </TableBodyCell>
            <TableBodyCell></TableBodyCell>
            <TableBodyCell></TableBodyCell>
            <TableBodyCell class="flex justify-start items-center gap-4">
              <a href="#">view {{ page }}</a>
              <a href="#">edit {{ page }}</a>
            </TableBodyCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
    <ContentBlock
      v-if="pages.advancedBuilder"
      title="HTML Pages"
      description="HTML Pages are pages that were created with the advanced page editor and built using HTML, CSS, and JavaScript"
    >
      <Table>
        <template #head>
          <TableHeadRow>
            <TableHeadCell>Page Name</TableHeadCell>
            <TableHeadCell>Date Created</TableHeadCell>
            <TableHeadCell>Date Modified</TableHeadCell>
            <TableHeadCell>Page Options</TableHeadCell>
          </TableHeadRow>
        </template>
        <template #body>
          <TableRow
            v-for="page in pages.advancedBuilder.map((page: string) => page.split('page.')[1])"
          >
            <TableBodyCell>
              {{ page }}
            </TableBodyCell>
            <TableBodyCell></TableBodyCell>
            <TableBodyCell></TableBodyCell>
            <TableBodyCell class="flex justify-start items-center gap-4">
              <a href="#">view {{ page }}</a>
              <a href="#">edit {{ page }}</a>
            </TableBodyCell>
          </TableRow>
        </template>
      </Table>
    </ContentBlock>
  </Container>
</template>
