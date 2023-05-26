<script setup lang="ts">
import { DocumentNode } from "graphql";

const props = defineProps<{
  title: string;
  description?: string;
  headers: { id: string; label: string; singleLine?: boolean }[];
  type: string;
  query: DocumentNode;
  filter?: object;
  rowMapper: Function;
  primaryActionLabel?: string;
  primaryActionPath?: string;
}>();

const pageSize = 10;
let pageNumber: Ref = ref(1);
let offset = computed(() => (pageNumber.value - 1) * pageSize);
let orderByColumn = ref(props.headers[0].id);
let orderby = {
  [orderByColumn.value]: "ASC",
};

const rows = ref([]);
const count = ref(0);

async function fetchRows() {
  const resp = await fetchGql(props.query, {
    ...props.filter,
    limit: pageSize,
    offset: offset.value,
    orderby,
  }).catch((e) => console.log(e));

  rows.value = resp.data[props.type]?.map(props.rowMapper);
  count.value = resp.data[`${props.type}_agg`].count;
}

fetchRows();

watch(orderByColumn, () => {
  orderby = {
    [orderByColumn.value]: "ASC",
  };
  fetchRows();
});

function setCurrentPage(newPageNumber: number) {
  pageNumber.value = newPageNumber;
  fetchRows();
}

let activeSideModal = ref("");
function setActiveSideModal(value: string) {
  activeSideModal.value = value;
}
</script>

<template>
  <ContentBlock :title="title" :description="description">
    <ButtonGroup
      v-if="count > pageSize || primaryActionPath"
      class="flex mb-5 flex-wrap"
    >
      <div class="grow">
        <NuxtLink v-if="primaryActionPath" :to="primaryActionPath">
          <Button :label="primaryActionLabel" type="tertiary" size="medium" />
        </NuxtLink>
      </div>
      <div v-if="count > pageSize" class="relative">
        <label
          class="block absolute text-body-xs top-2 left-6 pointer-events-none"
          for="sort-by"
        >
          Sort by
        </label>
        <select
          v-model="orderByColumn"
          name="sort-by"
          class="h-14 border border-gray-400 pb-2 pt-6 pl-6 pr-12 rounded-full appearance-none hover:bg-gray-100 hover:cursor-pointer bg-none"
        >
          <option v-for="header in headers" :value="header.id">
            {{ header.label }}
          </option>
        </select>
        <span class="absolute right-5 top-5 pointer-events-none">
          <BaseIcon name="caret-down" :width="20" />
        </span>
      </div>
    </ButtonGroup>

    <Table>
      <template #head>
        <TableHeadRow>
          <TableHead v-for="header in headers">
            <span class="hidden sm:table-cell">{{ header.label }}</span>
          </TableHead>
          <TableHead><span class="hidden sm:table-cell"></span></TableHead>
        </TableHeadRow>
      </template>
      <template #body>
        <TableRow
          v-for="row in rows"
          @click="setActiveSideModal(row[headers[0].id])"
        >
          <TableCell>
            <span>{{ row[headers[0].id] }}</span>
            <dl class="font-normal sm:hidden text-gray-900">
              <template v-for="header in headers.slice(1)">
                <dt class="font-bold mt-2.5">{{ header.label }}</dt>
                <dd>{{ row[header.id] }}</dd>
              </template>
            </dl>
          </TableCell>
          <TableCell
            v-for="header in headers.slice(1)"
            class="hidden sm:table-cell"
            :class="header.singleLine && 'w-full'"
            :style="header.singleLine && 'max-width: 0'"
          >
            <div :class="{ truncate: header.singleLine }">
              {{ row[header.id] }}
            </div>
          </TableCell>
          <TableCell class="hidden sm:table-cell">
            <IconButton icon="arrow-right" class="text-blue-500" />
          </TableCell>

          <SideModal
            :show="activeSideModal === row[headers[0].id]"
            :fullScreen="false"
            :slideInRight="true"
            @close="setActiveSideModal('')"
            buttonAlignment="right"
          >
            <!-- pass row id to allow slot implementer to fetch data and render side modal body data -->
            <slot :id="row.id"></slot>

            <template #footer>
              <NuxtLink :to="row._path">
                <Button type="secondary" size="small" label="Detail page" />
              </NuxtLink>
            </template>
          </SideModal>
        </TableRow>
      </template>
    </Table>
    <Pagination
      v-if="count > pageSize"
      :currentPage="pageNumber"
      :totalPages="Math.ceil(count / pageSize)"
      @update="setCurrentPage($event)"
      :prevent-default="true"
    />
  </ContentBlock>
</template>
