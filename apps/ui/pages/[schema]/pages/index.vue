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
const addPageModal = ref<InstanceType<typeof SideModal>>();
const showAddPageModal = ref<boolean>(false);

const disableSubmitButton = computed<boolean>(() => {
  return !/^[a-zA-Z]+$/g.exec(newPageName.value)
});

interface PageMetadata {
  [key: string]: {
    isSorted: boolean;
  }
}

const metadata = ref<PageMetadata>({
  'id': { isSorted: false },
  'name': { isSorted: false },
  'type': { isSorted: false },
  'dateCreated': { isSorted: false },
  'dateModified': { isSorted: false },
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
            id: (setting.key.split("page.")[1] as string).replaceAll(" ", "-"),
            name: setting.key.split("page.")[1],
            type: json.type,
            dateCreated: json.dateCreated,
            dateModified: json.dateModified
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
    newPageCode.dateCreated = newPageDate();

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

function handleSort($event: Record<string,string>) {
  Object.keys(metadata.value)
    .forEach((key: string) => metadata.value[key] = { isSorted: false });
  metadata.value[$event.column as string] = { isSorted: true }
  sortData($event.column as string, $event.sort === 'ASC')
}

function sortData(column: string, isAscending: boolean) {
  pages.value = pages.value.sort((a: Page,b: Page) => {
    const aValue = a[column as keyof Page] as string;
    const bValue = b[column as keyof Page] as string;
    if (isAscending) {
     if (aValue === "" || aValue === undefined) {
      return 1;
     } else if (bValue === "" || bValue === undefined) {
      return -1;
     } else {
      return aValue?.localeCompare(bValue);
     }
    } else {
      if (aValue === "" || aValue === undefined) {
       return -1;
      } else if (bValue === "" || bValue === undefined) {
       return 1;
      } else {
       return bValue?.localeCompare(aValue);
      }
    }
  });
}

const crumbs: Record<string, string> = {};
if (schema) {
  crumbs[schema] = `/${schema}`;
}
crumbs["Pages"] = "";
</script>

<template>
  <div class="mx-auto px-0 lg:px-7.5">
    <PageHeader title="Pages" align="left">
      <template #prefix>
        <BreadCrumbs align="left" :crumbs="crumbs" />
      </template>
    </PageHeader>
    <div class="flex justify-end gap-2.5 mb-7.5">
      <Button
        type="primary"
        icon="add-circle"
        @click="addPageModal?.showModal()"
      >
        Add page
      </Button>
    </div>
    <div
      v-if="pages"
      class="relative rounded-b-theme border border-theme border-color-theme"
    >
      <table class="text-left w-full table-fixed">
        <thead>
          <TableHeadRow
            class="[&_th]:text-table-column-header [&_th]:font-normal [&_th]:align-middle"
          >
            <TableHeadCell class="absolute left-0 w-[1px] !p-0 m-0 border-none">
              <span class="sr-only">page options</span>
            </TableHeadCell>
            <TableHeadCell v-for="key in Object.keys(metadata)" class="w-full">
              <TableTheadSortButton
                :label="key"
                :is-sorted="(metadata[key]?.isSorted as boolean)"
                @sort="handleSort"
              />
            </TableHeadCell>
          </TableHeadRow>
        </thead>
        <tbody
          lass="mb-3 [&_tr:last-child_td]:border-none [&_tr:last-child_td]:pb-last-row-cell"
        >
          <tr
            v-for="page in pages"
            v-if="pages"
            class="group hover:cursor-pointer"
          >
            <TableBodyCell
              class="absolute left-0 h-10 w-[150px] z-10 text-table-row bg-hover group-hover:bg-hover invisible group-hover:visible border-none mt-1"
            >
              <div
                class="flex flex-row items-center justify-start flex-nowrap gap-0 [&_button]:relative [&_button]:mt-[-11px]"
              >
                <NuxtLink
                  :to="`./pages/${page.id}/edit`"
                  class="block flex items-center justify-center rounded-full h-10 w-10 hover:text-button-secondary-hover focus:text-button-secondary-hover -mt-2.5"
                  v-tooltip.bottom="`Edit`"
                >
                  <BaseIcon name="Edit" :width="18" />
                  <span class="sr-only">Edit {{ page.name }} </span>
                </NuxtLink>
                <Button
                  type="inline"
                  :icon-only="true"
                  icon="Trash"
                  label="Delete"
                  size="small"
                  class="hover:bg-button-secondary-hover focus:bg-button-secondary-hover"
                  @click="deletePage(page.name)"
                />
                <NuxtLink
                  :to="`./pages/${page.id}`"
                  class="block flex items-center justify-center rounded-full h-10 w-10 hover:text-button-secondary-hover focus:text-button-secondary-hover -mt-2.5"
                  v-tooltip.bottom="`Preview`"
                >
                  <BaseIcon name="Preview" :width="18" />
                  <span class="sr-only">View {{ page.name }} </span>
                </NuxtLink>
              </div>
            </TableBodyCell>
            <TableBodyCell
              v-for="key in Object.keys(metadata)"
              class="text-table-row group-hover:bg-hover"
            >
              <NuxtLink
                v-if="key === 'name'"
                :to="`./pages/${page.name}`"
                class="hover:underline focus:underline group-hover:underline group-focus:underline"
              >
                {{ page.name }}
              </NuxtLink>
              <span v-else>{{ page[(key as keyof Page)] }}</span>
            </TableBodyCell>
          </tr>
        </tbody>
      </table>
      <div
        v-if="!pages.length"
        id="page-manager-pages-no-results-message"
        class="flex justify-center items-center py-5"
        role="status"
        aria-atomic="true"
      >
        <TextNoResultsMessage label="No pages found" />
      </div>
    </div>
    <SideModal
      ref="addPageModal"
      :show="showAddPageModal"
      :slide-in-right="true"
      :include-footer="false"
      :full-screen="false"
    >
      <ContentBlockModal title="Add page" class="text-title-contrast">
        <form @click="$event.stopPropagation()" @submit.prevent>
          <legend class="sr-only">Add a new page</legend>
          <label class="font-bold">
            Page Name
          </label>
          <p>Name cannot contain and numbers, characters, or spaces</p>
          <InputString
            id="manage-pages-new-page-name"
            placeholder="MyNewPage"
            class="mt-2"
            v-model="newPageName"
          />
          <div class="flex justify-start items-center gap-2.5 mt-5">
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
  </div>
</template>
