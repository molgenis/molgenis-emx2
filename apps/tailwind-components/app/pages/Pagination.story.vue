<template>
  <Pagination :currentPage="1" :totalPages="10" @update="updatePage" />

  <Pagination
    :currentPage="24"
    :totalPages="999"
    :preventDefault="true"
    @update="updatePage"
  />

  <p>jumpToEdge = true:</p>
  <Pagination
    :currentPage="24"
    :totalPages="999"
    :preventDefault="true"
    :jumpToEdge="true"
    @update="updatePage"
  />

  <p>showPageSizeSelector = true:</p>
  <Pagination
    :currentPage="24"
    :totalPages="999"
    :preventDefault="true"
    :jumpToEdge="true"
    :showPageSizeSelector="true"
    @update="updatePage"
  />

  <hr class="my-3" />

  <span>inverted = true:</span>
  <Pagination
    :currentPage="1"
    :totalPages="10"
    :inverted="true"
    @update="updatePage"
  />
</template>

<script setup lang="ts">
function updatePage(page: number) {
  alert(`Navigating to page ${page}`);
}
</script>
