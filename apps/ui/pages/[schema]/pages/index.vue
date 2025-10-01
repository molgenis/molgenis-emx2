<script lang="ts" setup>
import { useFetch } from "#app";
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";

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

useHead({ title: `Pages - ${schema} - Molgenis` });

const newPageName = ref<string>("");
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
      <Table class="mb-7.5">
        <template #head>
          <TableHeadRow>
            <TableHeadCell>
              Name
            </TableHeadCell>
            <TableHeadCell>
              Type
            </TableHeadCell>
            <TableHeadCell>
              Date Created
            </TableHeadCell>
            <TableHeadCell>
              Date Modified
            </TableHeadCell>
            <TableHeadCell>
              <span class="sr-only">Page Options</span>
            </TableHeadCell>
          </TableHeadRow>
        </template>
        <template #body>
          <tr v-for="page in pages">
            <TableBodyCell>
              <NuxtLink
                :to="`./pages/${page}`"
                class="hover:underline focus:underline"
              >
                {{ page }}
              </NuxtLink>
            </TableBodyCell>
            <TableBodyCell />
            <TableBodyCell />
            <TableBodyCell />
            <TableBodyCell class="flex justify-end items-center">
              <NuxtLink
                :to="`./pages/${page}/edit`"
                class="flex items-center justify-center rounded-full p-[8px] h-14 w-14 hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                v-tooltip.bottom="`Edit`"
              >
                <BaseIcon name="Edit" :width="24" />
                <span class="sr-only">Edit {{ page }} </span>
              </NuxtLink>
              <Button
                type="inline"
                :icon-only="true"
                icon="Trash"
                label="Delete"
                class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
              />
              <NuxtLink
                :to="`./pages/${page}`"
                class="flex items-center justify-center rounded-full p-[8px] h-14 w-14 hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                v-tooltip.bottom="`View`"
              >
                <BaseIcon name="ArrowRight" :width="24" />
                <span class="sr-only">View {{ page }} </span>
              </NuxtLink>
            </TableBodyCell>
          </tr>
        </template>
      </Table>
      <form @click.stop>
        <legend
          class="text-heading-4xl text-title-contrast font-display mb-2.5"
        >
          Add a new page
        </legend>
        <div class="flex justify-start items-end gap-5">
          <div>
            <label class="text-title-contrast font-bold">
              Page Name
            </label>
            <InputString
              id="manage-pages-new-page-name"
              class="mt-2"
              v-model="newPageName"
            />
          </div>
          <Button type="primary" icon="Plus" v-if="newPageName !== ''">
            Create new page
          </Button>
        </div>
      </form>
    </ContentBlock>
  </Container>
</template>
