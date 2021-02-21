<template>
  <nav aria-label="Pagination">
    <ul class="pagination justify-content-center mb-0">
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="page = 1">
          <span aria-hidden="true">&laquo;</span>
          <span class="sr-only">First</span></a
        >
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="page = Math.max(page - 1, 1)"
        >
          <span aria-hidden="true">&lsaquo;</span>
          <span class="sr-only">Previous</span>
        </a>
      </li>
      <li class="page-item">
        <a class="page-link text-nowrap" href="#"
          >{{ (page - 1) * limit + 1 }} - {{ Math.min(count, page * limit) }} of
          {{ count }}</a
        >
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="page = Math.min(page + 1, totalPages)"
        >
          <span aria-hidden="true">&rsaquo;</span>
          <span class="sr-only">Next</span></a
        >
      </li>
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="page = totalPages">
          <span aria-hidden="true">&raquo;</span>
          <span class="sr-only">Last</span></a
        >
      </li>
    </ul>
  </nav>
</template>

<style scope>
.page-item {
  border: none;
}
</style>

<script>
export default {
  props: {
    count: Number,
    limit: { type: Number, default: 10 },
    defaultValue: { type: Number, default: 1 },
  },
  data: function () {
    return {
      page: 1,
    };
  },
  computed: {
    offset() {
      return this.limit * (this.page - 1);
    },
    totalPages() {
      return Math.ceil(this.count / this.limit);
    },
  },
  watch: {
    page() {
      this.$emit("input", this.page);
    },
    count() {
      //reset page to within range in case count changes
      if (this.page > this.totalPages) {
        this.page = 1;
      }
    },
  },
  created() {
    if (this.defaultValue) {
      this.page = this.defaultValue;
    }
    this.$emit("input", this.page);
  },
};
</script>

<docs>
Example
```
<template>
  <div>
    <Pagination v-model="page" :count="29"/>
    page = {{ page }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        page: null
      }
    }
  }
</script>
```
Example with default and limit
```
<template>
  <div>
    <Pagination v-model="page" :count="29" :limit="5" :defaultValue="page"/>
    page = {{ page }}
  </div>
</template>
<script>
  export default {
    data() {
      return {
        page: 3
      }
    }
  }
</script>
```
</docs>
