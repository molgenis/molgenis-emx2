<template>
  <div id="app" class="d-flex">
    <template v-if="$route.path === '/'">
      <router-view name="sidebar" :docsMap="$docsMap"></router-view>
      <div
        id="page-content-wrapper"
        class="container-fluid overflow-auto vh-100"
      >
        <div v-for="name in docNames" :key="name" :id="name">
          <h2>{{ name }}</h2>
          <router-link class="float-right" :to="'/component/' + name">
            focus
          </router-link>
          <div class="border-top border-primary pt-4 mb-4">
            <router-view :name="name"></router-view>
          </div>
        </div>
      </div>
    </template>
    <div class="container" v-else>
      <h2>{{ currentRouteName }}</h2>
      <router-link :to="'/'">Back to listing</router-link>
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
export default {
  computed: {
    docNames() {
      return Object.keys(this.$docsMap);
    },
    currentRouteName() {
      return this.$route.path;
    },
  },
};
</script>

<style>
html, body {
  min-height: 100% !important;
  height: 100% !important;
  overflow: hidden;
}

#sidebar-wrapper .sidebar-heading {
  padding: 0.875rem 1.25rem;
  font-size: 1.2rem;
}

#sidebar-wrapper .list-group {
  width: 15rem;
}

#page-content-wrapper {
  min-width: 75vw;
}
</style>
