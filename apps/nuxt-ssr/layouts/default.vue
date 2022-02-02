<template>
<div class="wrapper d-flex flex-column">
  <Menu :menu="menu"/>
  <Nuxt class="flex-fill" />
  <molgenis-footer></molgenis-footer>
</div>
</template>

<style>
body, div.wrapper {
   min-height:100vh;
}

</style>

<script>
export default {
  async fetch ({store}) {
    return store.dispatch('fetchSession')
  },
  computed: {
    menu () {
      return this.$store.state.menu
    },
    schema () {
      return this.$store.state.schema
    }
  },
}
</script>