<template>
  <nav aria-label="Pagination">
    <ul class="pagination justify-content-center mb-0">
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="$emit('update:modelValue', 1)">
          <span aria-hidden="true">&laquo;</span>
          <span class="sr-only">First</span></a>
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="$emit('update:modelValue', Math.max(value - 1, 1))"
        >
          <span aria-hidden="true">&lsaquo;</span>
          <span class="sr-only">Previous</span>
        </a>
      </li>
      <li class="page-item">
        <a
          class="page-link text-nowrap" href="#"
        >{{ (value - 1) * limit + 1 }} -
          {{ Math.min(count, value * limit) }} of {{ count }}</a>
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="$emit('update:modelValue', Math.min(value + 1, totalPages))"
        >
          <span aria-hidden="true">&rsaquo;</span>
          <span class="sr-only">Next</span></a>
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="$emit('update:modelValue', totalPages)"
        >
          <span aria-hidden="true">&raquo;</span>
          <span class="sr-only">Last</span></a>
      </li>
    </ul>
  </nav>
</template>

<script>
export default {
  props: {
    count: Number,
    limit: {type: Number, default: 10},
    value: {type: Number, default: 1},
  },
  emits: ['update:modelValue'],
  computed: {
    offset() {
      return this.limit * (this.value - 1)
    },
    totalPages() {
      return Math.ceil(this.count / this.limit)
    },
  },
  watch: {
    count() {
      // reset page to within range in case count changes
      if (this.page > this.totalPages) {
        this.$emit('update:modelValue', 1)
      }
    },
    value() {
      if (this.value < 1) {
        this.$emit('update:modelValue', 1)
      }
    },
  },
  created() {
    if (this.value < 1) {
      this.$emit('update:modelValue', 1)
    }
  },
}
</script>

<style scope>
.page-item {
  border: none;
}
</style>

