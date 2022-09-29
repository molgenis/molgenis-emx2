<template>
  <nav aria-label="Pagination">
    <ul class="pagination justify-content-center mb-0">
      <li class="page-item" :class="{ disabled: isFirstPage }">
        <a
          class="page-link"
          href="#"
          @click.prevent="emitValue(1, isFirstPage)"
        >
          <span aria-hidden="true">&laquo;</span>
          <span class="sr-only">First</span></a
        >
      </li>
      <li class="page-item" :class="{ disabled: isFirstPage }">
        <a
          class="page-link"
          href="#"
          @click.prevent="emitValue(Math.max(value - 1, 1), isFirstPage)"
        >
          <span aria-hidden="true">&lsaquo;</span>
          <span class="sr-only">Previous</span>
        </a>
      </li>
      <li class="page-item disabled">
        <a class="page-link text-nowrap" href="#">{{
          rowRange(value, limit, count)
        }}</a>
      </li>
      <li class="page-item" :class="{ disabled: isLastPage }">
        <a
          class="page-link"
          href="#"
          @click.prevent="
            emitValue(Math.min(value + 1, totalPages), isLastPage)
          "
        >
          <span aria-hidden="true">&rsaquo;</span>
          <span class="sr-only">Next</span></a
        >
      </li>
      <li class="page-item" :class="{ disabled: isLastPage }">
        <a
          class="page-link"
          href="#"
          @click.prevent="emitValue(totalPages, isLastPage)"
        >
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
    value: { type: Number, default: 1 },
    count: Number,
    limit: { type: Number, default: 10 },
  },
  methods: {
    emitValue(page, isDisabled) {
      if (!isDisabled) {
        this.$emit("input", page);
      }
    },
    rowRange(value, limit, count) {
      if (count === 0) {
        return "-";
      } else {
        const from = (value - 1) * limit + 1;
        const to = Math.min(count, value * limit);
        return `${from} - ${to} of ${count}`;
      }
    },
  },
  computed: {
    totalPages() {
      return Math.ceil(this.count / this.limit);
    },
    isFirstPage() {
      return this.value === 1;
    },
    isLastPage() {
      return this.value === this.totalPages || this.count === 0;
    },
  },
  watch: {
    value() {
      if (this.value < 1) {
        this.$emit("input", 1);
      }
    },
    count() {
      //reset page to within range in case count changes
      if (this.page > this.totalPages) {
        this.$emit("input", 1);
      }
    },
  },
  created() {
    if (this.value < 1) {
      this.$emit("input", 1);
    }
  },
};
</script>

<docs>
</docs>
