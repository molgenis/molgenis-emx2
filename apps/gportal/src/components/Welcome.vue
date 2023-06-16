<template>
  <header>
    <h1>GDI Portal</h1>
  </header>
  <main>
    <section aria-labelledby="get-started-title">
      <h2 id="get-started-title">Get Started</h2>
      <p>
        Welcome to the <strong>GDI Portal</strong>. In the portal, you can
        search for datasets and records to find the data of interested. If you
        would like to work with some of the data, you can submit a request of
        access on the search page.
      </p>
      <nav aria-labelledby="search-page-nav">
        <h3 id="search-page-nav">Search for data</h3>
        <ul>
          <li>
            <RouterLink :to="{ name: 'datasets' }">Find datasets</RouterLink>
          </li>
          <li>
            <RouterLink :to="{ name: 'beacon' }">Go to beacon</RouterLink>
          </li>
        </ul>
      </nav>
    </section>
  </main>
</template>

<script setup>
import { RouterLink } from "vue-router";
</script>
