<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { HomeIcon } from "@heroicons/vue/24/outline";

interface IBreadcrumbs {
  name: string;
  label: string;
}

const route = useRoute();
const breadcrumbs = computed<IBreadcrumbs[] | undefined>(() => {
  if (route.meta.breadcrumbs) {
    return route.meta.breadcrumbs as IBreadcrumbs[];
  }
});
</script>

<template>
  <div class="breadcrumbs-container page-section">
    <nav class="breadcrumbs page-section-content width-medium">
      <ul>
        <li>
          <router-link :to="{ name: 'home' }">
            <span class="visually-hidden">Go to home page</span>
            <HomeIcon class="heroicons heroicons-outline heroicons-home" />
          </router-link>
        </li>
        <li v-for="crumb in breadcrumbs">
          <router-link :to="{ name: crumb.name }">
            {{ crumb.label }}
          </router-link>
        </li>
      </ul>
    </nav>
  </div>
</template>

<style lang="css">
.breadcrumbs-container {
  box-sizing: content-box;
  padding: 1em;
}

.breadcrumbs-container .breadcrumbs ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 1em;
}

.breadcrumbs-container .breadcrumbs ul li {
  position: relative;
}

.breadcrumbs-container .breadcrumbs ul li .heroicons {
  width: 21px;
  height: 21px;
  margin-top: -3px;
}

.breadcrumbs-container .breadcrumbs ul li::after {
  content: "/";
  margin-left: 1em;
  color: var(--gray-600);
}

.breadcrumbs-container .breadcrumbs ul li:last-child::after {
  content: none;
}
</style>
