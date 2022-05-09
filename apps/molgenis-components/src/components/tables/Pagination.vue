<template>
  <nav aria-label="Pagination">
    <ul class="pagination justify-content-center mb-0">
      <li class="page-item">
        <a class="page-link" href="#" @click.prevent="$emit('input', 1)">
          <span aria-hidden="true">&laquo;</span>
          <span class="sr-only">First</span></a
        >
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="$emit('input', Math.max(value - 1, 1))"
        >
          <span aria-hidden="true">&lsaquo;</span>
          <span class="sr-only">Previous</span>
        </a>
      </li>
      <li class="page-item">
        <a class="page-link text-nowrap" href="#"
          >{{ (value - 1) * limit + 1 }} -
          {{ Math.min(count, value * limit) }} of {{ count }}</a
        >
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="$emit('input', Math.min(value + 1, totalPages))"
        >
          <span aria-hidden="true">&rsaquo;</span>
          <span class="sr-only">Next</span></a
        >
      </li>
      <li class="page-item">
        <a
          class="page-link"
          href="#"
          @click.prevent="$emit('input', totalPages)"
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
  computed: {
    offset() {
      return this.limit * (this.value - 1);
    },
    totalPages() {
      return Math.ceil(this.count / this.limit);
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

<docs>
  <template>
    <demo-item>
      <pagination v-model="page" :count="250"/>
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
</docs>
