<script setup lang="ts">
import { ref } from "vue";
import InlinePagination from "../../components/display/InlinePagination.vue";

const interactivePage = ref(1);
const interactiveTotalPages = ref(7);
</script>

<template>
  <div class="p-5 space-y-10">
    <h1 class="text-2xl font-bold">InlinePagination Component</h1>
    <p class="text-body-muted">
      Compact prev / current / next control. Prev is disabled on page 1, Next is
      disabled on the last page. Emits update:page when a button is clicked.
    </p>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Interactive</h2>
      <p class="text-sm text-body-muted">
        Page {{ interactivePage }} / {{ interactiveTotalPages }} — click Prev /
        Next to navigate
      </p>
      <div class="border border-divider rounded p-4 flex items-center gap-4">
        <InlinePagination
          :current-page="interactivePage"
          :total-pages="interactiveTotalPages"
          @update:page="interactivePage = $event"
        />
        <div class="flex gap-2 text-sm">
          <label class="text-body-muted">Total pages:</label>
          <input
            v-model.number="interactiveTotalPages"
            type="number"
            min="1"
            max="50"
            class="w-16 border border-input rounded-input px-2 py-0.5"
          />
        </div>
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">First page — Prev disabled</h2>
      <div class="border border-divider rounded p-4">
        <InlinePagination :current-page="1" :total-pages="5" />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Middle page — both buttons enabled</h2>
      <div class="border border-divider rounded p-4">
        <InlinePagination :current-page="3" :total-pages="5" />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Last page — Next disabled</h2>
      <div class="border border-divider rounded p-4">
        <InlinePagination :current-page="5" :total-pages="5" />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Single page — both buttons disabled</h2>
      <p class="text-sm text-body-muted">
        totalPages=1, currentPage=1 → no navigation possible
      </p>
      <div class="border border-divider rounded p-4">
        <InlinePagination :current-page="1" :total-pages="1" />
      </div>
    </section>

    <section class="space-y-2">
      <h2 class="text-lg font-semibold">Many pages — truncated display</h2>
      <p class="text-sm text-body-muted">
        Shows page X / Y label; no auto-truncation — all pages reachable via
        repeated clicks
      </p>
      <div class="border border-divider rounded p-4">
        <InlinePagination :current-page="42" :total-pages="250" />
      </div>
    </section>
  </div>
</template>
