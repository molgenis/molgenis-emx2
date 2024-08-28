<script setup lang="ts">
import type {
  IColumn,
  ITableMetaData,
} from "../../../metadata-utils/src/types";

const props = defineProps<{
  metaData: ITableMetaData;
}>();

interface IChapter {
  title: string | "_NO_CHAPTERS";
  columns: IColumn[];
}

const chapters = computed(() => {
  return props.metaData.columns.reduce((acc, column) => {
    if (column.columnType === "HEADING") {
      acc.push({
        title: column.id,
        columns: [],
      });
    } else {
      if (acc.length === 0) {
        acc.push({
          title: "_NO_CHAPTERS",
          columns: [],
        });
      }
      acc[acc.length - 1].columns.push(column);
    }
    return acc;
  }, [] as IChapter[]);
});
</script>
<template>
  <div>
    <div class="first:pt-0 pt-10" v-for="chapter in chapters">
      <h2
        class="font-display md:text-heading-5xl text-heading-5xl text-title-contrast pb-8"
        v-if="chapter.title !== '_NO_CHAPTERS'"
      >
        {{ chapter.title }}
      </h2>
      <div class="pb-8" v-for="column in chapter.columns">
        <FormField :column="column"></FormField>
      </div>
    </div>
  </div>
</template>
