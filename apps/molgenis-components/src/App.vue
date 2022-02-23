<template>
  <div id="app" class="d-flex">
    <template v-if="$route.path === '/'">
      <router-view name="sidebar"></router-view>
      <div id="page-content-wrapper" class="m-3 container-fluid">
        <router-view
          v-for="name in docNames"
          :key="name"
          :name="name"
        ></router-view>
      </div>
    </template>
    <div class="container" v-else>
      <router-view></router-view>
    </div>
  </div>
</template>

<script>
export default {
  computed: {
    docNames() {
      return this.$docNames;
    },
  },
};
</script>



<style>
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
