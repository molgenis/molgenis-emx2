<template>
  <Pagination :currentPage="1" :totalPages="10" @update="updatePage" />

  <Pagination
    :currentPage="24"
    :totalPages="999"
    type="white"
    :preventDefault="true"
    @update="updatePage"
  />
</template>

<script setup lang="ts">
function updatePage(page: number) {
  alert(`Navigating to page ${page}`);
}
</script>
