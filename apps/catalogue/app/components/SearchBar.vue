<script setup lang="ts">
import { ref, useId } from 'vue';
import Button from '../../../tailwind-components/app/components/Button.vue';
withDefaults(defineProps<{
  placeholder?: string;
}>(), {
  placeholder: "Search variable. cohort etc"
});
defineEmits(["submitSearch"]);

const searchQuery = ref("");
const id = useId();

</script>

<template>
  <form class="relative" @submit.prevent="$emit('submitSearch')">
    <input
      type="search"
      v-model="searchQuery"
      :id="id"
      class="w-full pr-32 font-sans text-gray-900 bg-white outline-none rounded-search-input h-15 ring-red-500 pl-7 border-search-input focus:border-white shadow-search-input focus:shadow-search-input hover:shadow-search-input"
      :placeholder="placeholder"
    />
    <Button
      type="primary"
      @click="$emit('submitSearch', searchQuery)"
      class="rounded-search-button absolute top-0 right-0 flex items-center pl-8 pr-6 tracking-wider border-search-button border-[0px] h-15"
    >   
      Search All
    </Button>
  </form>
</template>
