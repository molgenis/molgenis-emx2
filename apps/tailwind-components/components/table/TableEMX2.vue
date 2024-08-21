<script setup lang="ts">
import type { ITableSettings } from "~/types/types";
import type { IColumn } from "../../../metadata-utils/src/types";

const props = withDefaults(
  defineProps<{
    columns: IColumn[];
    rows: Record<string, any>[];
    settings?: ITableSettings;
  }>(),
  {
    settings: {
      page: 1,
      orderby: { column: "", direction: "ASC" },
      search: "",
    },
  }
);

const emit = defineEmits(["update:settings"]);

function handleSortRequest(columnId: string) {
  let direction = "ASC";
  if (props.settings?.orderby?.column === columnId) {
    direction = props.settings.orderby.direction === "ASC" ? "DESC" : "ASC";
  }
  emit("update:settings", {
    ...props.settings,
    orderby: { column: columnId, direction },
  });
}

function handleSearchRequest(search: string) {
  emit("update:settings", {
    ...props.settings,
    search,
  });
}
</script>
<template>
  <div class="overflow-x-auto overscroll-x-contain">
    <FilterSearch
      :modelValue="settings.search"
      @update:modelValue="handleSearchRequest"
      :inverted="true"
    ></FilterSearch>
    <table class="text-left table-fixed w-full">
      <thead>
        <tr class="">
          <th
            v-for="column in columns"
            class="py-2.5 px-2.5 border-b border-gray-200 first:pl-0 last:pr-0 sm:first:pl-2.5 sm:last:pr-2.5 text-left w-64"
          >
            <div
              class="overflow-ellipsis whitespace-nowrap overflow-hidden hover:cursor-pointer"
              @click="handleSortRequest(column.id)"
            >
              {{ column.label }}
              <ArrowUp
                v-if="
                  column.id === settings?.orderby?.column &&
                  settings?.orderby?.direction === 'ASC'
                "
                class="w-4 h-4 inline-block"
              />
              <ArrowDown
                v-if="
                  column.id === settings?.orderby?.column &&
                  settings?.orderby?.direction === 'DESC'
                "
                class="w-4 h-4 inline-block"
              />
            </div>
            <!--  -->
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows">
          <TableCellTypesEMX2
            v-for="column in columns"
            :metaData="column"
            :data="row[column.id]"
          />
        </tr>
      </tbody>
    </table>
  </div>
</template>
