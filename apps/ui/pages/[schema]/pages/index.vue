<script lang="ts" setup>
import { ref, computed } from "vue";
import { useRoute } from "vue-router";
import { useHead } from "#app";
import { type PageBuilderContent, newPageContentObject, newPageDate } from "~/util/pages";
import type { SideModal } from "#build/components";

type Resp<T> = {
  data: Record<string, T>;
};

interface Setting {
  key: string;
  value: string;
}

interface Page {
  id: string;
  name: string;
  type?: string;
  dateCreated?: string;
  dateModified?: string;
}

interface ModelStatus {
  type: "error" | "success",
  message: string;
}

const route = useRoute();
const schema = Array.isArray(route.params.schema)
  ? route.params.schema[0]
  : route.params.schema ?? "";

useHead({ title: `Pages - ${schema} - Molgenis` });

const newPageName = ref<string>("");
const isSaving = ref<boolean>(false);
const pages = ref<Page[]>([]);
const showStatusModal = ref<boolean>(false);
const statusModal = ref<InstanceType<typeof SideModal>>();
const statusModalData = ref<ModelStatus>({type: "error", message: ""});

const disableSubmitButton = computed<boolean>(() => {
  return !/^[a-zA-Z]+$/g.exec(newPageName.value)
});

function fetchPages () {
  $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
      query: "{_settings{key value}}"
    }
  })
  .then((data: Resp<Setting[]>) => {
    pages.value = data.data?._settings
    ?.filter((setting: Setting) => {
      return setting.key.startsWith("page.");
    })
    .map((setting: Setting) => {
        try {
          const json = JSON.parse(setting?.value) as PageBuilderContent;
          return Object.assign({}, {
            id: (setting.key.split("page.")[1] as string),
            name: setting.key.split("page.")[1],
            type: json._meta?.type,
            dateCreated: json._meta?.dateCreated,
            dateModified: json._meta?.dateModified
          }) as Page;
        } catch (error) {
          console.warn('Unable to parse', setting.key, '\n\n',error);
        }
      }) as Page[]
  })
  .catch((err) => {
    statusModalData.value = {
      type: "error",
      message: err
    }
    showStatusModal.value = true;
  });
}

fetchPages();

function saveNewPage() {
  statusModalData.value.message = "";
  isSaving.value = true;

  const pageExists = pages.value.filter((page: Page) => page.name == newPageName.value);
  if (pageExists.length > 0) {
    statusModalData.value.message = `Unable to create new page: '${newPageName.value}' already exists`
    isSaving.value = false;
    showStatusModal.value = true;
  } else {

    const newPageCode = newPageContentObject("editor");
    newPageCode.html = `<h1>${newPageName.value}</h1>`;
    if (newPageCode._meta) {
      newPageCode._meta.dateCreated = newPageDate();
    }

  $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
    query: `mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){status message}}`,
    variables: {
      settings: {
        key: `page.${newPageName.value}`,
        value: JSON.stringify(newPageCode)
      }
    }
    }
  })
  .then(response => {
    if (response?.errors) {
      throw new Error(response.errors[0].message);
    }

    statusModalData.value = {
      type: "success",
      message:`Created new '${newPageName.value}'`
    };

    fetchPages();
  })
  .catch((err) => {
    statusModalData.value = {
      type: "error",
      message:`Unable to save '${newPageName.value}': ${err}`
    };
  })
  .finally(() => {
    isSaving.value = false;
    showStatusModal.value = true;
  });
  }
}

function deletePage (page: string) {
  statusModalData.value.message = "";

  $fetch(`/${schema}/graphql`, {
    method: "POST",
    body: {
    query: `mutation drop($settings:[DropSettingsInput]){drop(settings:$settings){status message}}`,
    variables: {
      settings: {
        key: `page.${page}`,
      }
    }
    }
  })
  .then((response) => {
    if (response?.errors) {
      throw new Error(`Unable to delete page '${page}': ${response.errors[0].message}`);
    }

    statusModalData.value = {
      type: "success",
      message:`Deleted page '${newPageName.value}'`
    };

    fetchPages();
  })
  .catch((err) => {
    statusModalData.value = {
      type: "error",
      message: err
    };
  })
  .finally(() => {
    showStatusModal.value = true;
  });
}


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
                :to="`./pages/${page.id}`"
                class="hover:underline focus:underline"
              >
                {{ page.name }}
              </NuxtLink>
            </TableBodyCell>
            <TableBodyCell>
              {{ page.type }}
            </TableBodyCell>
            <TableBodyCell>
              {{ page?.dateCreated }}
            </TableBodyCell>
            <TableBodyCell>
              {{ page?.dateModified }}
            </TableBodyCell>
            <TableBodyCell class="flex justify-end items-center">
              <NuxtLink
                :to="`./pages/${page.id}/edit`"
                class="flex items-center justify-center rounded-full p-[8px] h-14 w-14 hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                v-tooltip.bottom="`Edit`"
              >
                <BaseIcon name="Edit" :width="24" />
                <span class="sr-only">Edit {{ page.name }} </span>
              </NuxtLink>
              <Button
                type="inline"
                :icon-only="true"
                icon="Trash"
                label="Delete"
                class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                @click="deletePage(page.name)"
              />
              <NuxtLink
                :to="`./pages/${page.id}`"
                class="flex items-center justify-center rounded-full p-[8px] h-14 w-14 hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                v-tooltip.bottom="`View`"
              >
                <BaseIcon name="ArrowRight" :width="24" />
                <span class="sr-only">View {{ page.name }} </span>
              </NuxtLink>
            </TableBodyCell>
          </tr>
        </template>
      </Table>
      <form @click="$event.stopPropagation()" @submit.prevent>
        <legend
          class="text-heading-4xl text-title-contrast font-display mb-2.5"
        >
          Add a new page
        </legend>
        <div class="flex justify-start items-stretch gap-5 mb-2.5">
          <div class="self-end">
            <label class="text-title-contrast font-bold">
              Page Name
            </label>
            <p>Name cannot contain and numbers, characters, or spaces</p>
            <InputString
              id="manage-pages-new-page-name"
              placeholder="MyNewPage"
              class="mt-2"
              v-model="newPageName"
            />
          </div>
          <Button
            type="primary"
            icon="Plus"
            class="self-end"
            aria-describedby="manage-pages-new-page-name"
            :disabled="disableSubmitButton"
            :class="{
              'cursor-not-allowed': disableSubmitButton,
            }"
            @click="!disableSubmitButton ? saveNewPage() : null"
          >
            Create new page
          </Button>
          <div id="manage-pages-save-new-status" class="self-end">
            <div class="my-4" v-if="isSaving">
              <span class="sr-only">Loading</span>
              <BaseIcon
                name="ProgressActivity"
                :width="24"
                class="animate-spin"
              />
            </div>
          </div>
        </div>
      </form>
    </ContentBlock>
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
        :title="statusModalData.type"
        class="!bg-invalid text-invalid"
        :class="{
          '!bg-invalid text-invalid': statusModalData.type === 'error',
          '!bg-valid text-valid': statusModalData.type === 'success',
        }"
      >
        <p>{{ statusModalData.message }}</p>
      </ContentBlockModal>
    </SideModal>
  </Container>
</template>
