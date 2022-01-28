<template>
  <nav class="navbar bg-primary navbar-expand-lg navbar-dark">
  <a v-if="logo" class="navbar-brand">
      <img :src="logo" alt="brand-logo" height="30" />
  </a>
  <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
    <span class="navbar-toggler-icon"></span>
  </button>

  <div class="collapse navbar-collapse" id="navbarSupportedContent">
    <ul class="navbar-nav mr-auto">
      <li v-for="menuItem in menu" :key="menuItem.key" class="nav-item ">
        <a class="nav-link" :href="menuItem.href">{{menuItem.label}}</a>
      </li>
    </ul>
  </div>
</nav>
</template>

<script>
export default {
  name: "Menu",
  props: {
      menu: {
          type: Array
      },
      logo: {
          type: String,
          default: () => '/apps/styleguide/assets/img/molgenis_logo_white.png'
      }
  }
}
</script>

<style>


</style>