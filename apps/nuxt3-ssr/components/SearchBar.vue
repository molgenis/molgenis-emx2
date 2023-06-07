<script setup>
defineEmits(["submitSearch"]);

function submitSearch(event) {
  alert(`Search for ${event.target.elements[0].value}`);
}
</script>

<template>
  <form class="relative" @submit.prevent="submitSearch">
    <input
      type="search"
      class="w-full pr-32 font-sans text-gray-900 bg-white outline-none rounded-search-input h-15 ring-red-500 pl-7 border-search-input focus:border-white shadow-search-input focus:shadow-search-input hover:shadow-search-input"
      placeholder="Search variable. cohort etc" />
    <button
      class="rounded-search-button absolute top-0 right-0 flex items-center pl-8 pr-6 tracking-wider text-search-button border-search-button border-[1px] transition-colors bg-search-button h-15 font-display text-heading-2xl hover:text-search-button-hover">
      Search
    </button>
  </form>
</template>
