<template>
  <nav aria-label="Pagination">
    <ul class="pagination justify-content-center">
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="page = 1">First</a>
      </li>
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="page = Math.max(page - 1, 1)">Previous</a>
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
        >{{ (page-1)*limit+1}} - {{Math.min(count,page*limit+1)}} of {{count}}</a>
      </li>
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="page = Math.min(page + 1, totalPages)">Next</a>
      </li>
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="page = totalPages">Last</a>
      </li>
    </ul>
  </nav>
</template>

<script>
export default {
  props: {
    count: Number,
    limit: { type: Number, default: 10 }
  },
  data: function () {
    return {
      page: 1
    }
  },
  computed: {
    offset () {
      return this.limit * (this.page - 1)
    },
    totalPages () {
      return Math.ceil(this.count / this.limit)
    }
  },
  watch: {
    page () {
      this.$emit('input', this.page)
    }
  }
}
</script>

<docs>
Example
```
<Pagination :count="29"/>
```
</docs>
