<template>
  <demo-item id="pagination" label="Pagination">
    <pagination v-model="page" :count="250" />
    <div>page number: {{ page }}</div>
  </demo-item>
</template>

<script>
export default {
  data() {
    return {
      page: 3,
    };
  },
};
</script>