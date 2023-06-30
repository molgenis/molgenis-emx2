<template>
  <header class="py-3 mb-4 border-bottom bg-gradient-secondary">
    <div class="col-md-6 offset-md-2">
      <h1 class="title h4 font-weight-bold text-uppercase m-0">GDI Portal</h1>
      <h2 class="font-weight-light">Search for data and request access</h2>
    </div>
  </header>
  <main>
    <section
      aria-labelledby="get-started-title"
      class="col-md-8 offset-md-2 py-3"
    >
      <h2 id="get-started-title">Get Started</h2>
      <p>
        Welcome to the <strong>GDI Portal</strong>. In the portal, you can
        search for datasets and records to find the data of interested. If you
        would like to work with some of the data, you can submit a request of
        access on the search page.
      </p>
      <div class="card-deck">
        <div class="card">
          <div class="card-body">
            <h3 class="h5">Find Datasets</h3>
            <p>Explore datasets and collections, and request access.</p>
            <RouterLink
              :to="{ name: 'datasets' }"
              class="btn btn-outline-primary"
            >
              Get Started
              <ChevronRight />
            </RouterLink>
          </div>
        </div>
        <div class="card">
          <div class="card-body">
            <h3 class="h5">Find Records</h3>
            <p>Find records using Beacon and request access.</p>
            <RouterLink
              :to="{ name: 'beacon' }"
              class="btn btn-outline-primary"
            >
              Get started
              <ChevronRight />
            </RouterLink>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<script setup>
import { RouterLink } from "vue-router";
import ChevronRight from "./icons/chevron-right.vue";
</script>

<style lang="scss">
.card {
  .heroicons.chevron-right {
    $size: 14px;
    width: $size;
    height: $size;
    stroke-width: 3px;
  }
}
</style>
