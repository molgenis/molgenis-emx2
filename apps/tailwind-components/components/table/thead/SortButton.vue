<script lang="ts" setup>
import { ref, watch } from "vue";
type sortOptions = "ASC" | "DESC";

const props = withDefaults(
  defineProps<{
    label: string;
    isSorted: boolean;
  }>(),
  {
    isSorted: false,
  }
);

const emits = defineEmits<{
  (e: "sort", value: Record<string, string>): void;
}>();

const sort = ref<sortOptions | string>("");

function onClick() {
  if (sort.value === "") {
    sort.value = "ASC";
  } else if (sort.value === "ASC") {
    sort.value = "DESC";
  } else {
    sort.value = "ASC";
  }
  emits("sort", { column: props.label, sort: sort.value });
}

watch(
  () => props.isSorted,
  () => {
    if (!props.isSorted) {
      sort.value = "";
    }
  }
);
</script>

<template>
  <div class="flex justify-start items-center gap-1">
    <button
      class="overflow-ellipsis whitespace-nowrap max-w-56 overflow-hidden inline-block text-left text-table-column-header font-normal align-middle"
      @click="onClick"
      :ariaSort="['ASC', 'DESC'].includes(sort) ? label : 'none'"
    >
      <span>{{ label }}</span>
    </button>
    <ArrowUp
      v-if="sort === 'ASC'"
      aria-hidden="true"
      class="h-4 w-4 text-table-column-header font-normal"
    />
    <ArrowDown
      v-if="sort === 'DESC'"
      aria-hidden="true"
      class="h-4 w-4 text-table-column-header font-normal"
    />
  </div>
</template>
