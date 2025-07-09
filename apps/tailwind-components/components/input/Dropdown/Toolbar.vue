<script lang="ts" setup>
import { ref, watch } from "vue";
import { InputSearch } from "#components";

const props = defineProps<{
  id: string;
}>();

const emits = defineEmits(["search"]);

const searchTerm = ref<string>("");

watch(searchTerm, () => emits("search", searchTerm.value));
</script>

<template>
  <div
    class="bg-input flex flex-wrap md:flex-nowrap justify-start items-center gap-4 p-2"
  >
    <InputSearch
      :id="`${id}-dropdown-search`"
      placeholder="Search"
      v-model="searchTerm"
    />
  </div>
</template>
