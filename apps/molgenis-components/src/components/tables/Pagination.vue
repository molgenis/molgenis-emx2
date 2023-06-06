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
          @click.prevent="emitValue(Math.max(modelValue - 1, 1), isFirstPage)"
        >
          <span aria-hidden="true">&lsaquo;</span>
          <span class="sr-only">Previous</span>
        </a>
      </li>
      <li class="page-item disabled">
        <a class="page-link text-nowrap" href="#">
          {{ rowRange(modelValue, limit, count) }}
        </a>
      </li>
      <li class="page-item" :class="{ disabled: isLastPage }">
        <a
          class="page-link"
          href="#"
          @click.prevent="
            emitValue(Math.min(modelValue + 1, totalPages), isLastPage)
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

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  props: {
    /** current page */
    modelValue: { type: Number, default: 1 },
    /** total records, i.e. sql count */
    count: { type: Number, required: true },
    /** number of records per page, i.e. sql limit */
    limit: { type: Number, default: 10 },
  },
  methods: {
    emitValue(page: number, isDisabled: boolean) {
      if (!isDisabled) {
        this.$emit("update:modelValue", page);
      }
    },
    rowRange(value: number, limit: number, count: number) {
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
      return this.modelValue === 1;
    },
    isLastPage() {
      return this.modelValue === this.totalPages || this.count === 0;
    },
  },
  watch: {
    value() {
      if (this.modelValue < 1) {
        this.$emit("update:modelValue", 1);
      }
    },
    count() {
      //reset page to within range in case count changes
      if (this.modelValue > this.totalPages) {
        this.$emit("update:modelValue", 1);
      }
    },
  },
  created() {
    if (this.modelValue < 1) {
      this.$emit("update:modelValue", 1);
    }
  },
});
</script>

<docs>
<template>

  <div>
    <demo-item>
      <Pagination v-model="pageValue1" :count="29" />
      page = {{ pageValue1 }}
    </demo-item>

    <demo-item>
      <pagination v-model="pageValue2" :count="250" />
      <div>page number: {{ pageValue2 }}</div>
    </demo-item>
  </div>

</template>

<script>
  export default {
    data() {
      return {
        pageValue1: null,
        pageValue2: 3,
      }
    }
  }
</script>
</docs>
