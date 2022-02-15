<template>
<div class="wrapper d-flex flex-column">
  <Menu :brandHref=brandHref :menu="menu"/>
  <Nuxt class="flex-fill" />
  <molgenis-footer class="footer"></molgenis-footer>
</div>
</template>

<style>
body, div.wrapper {
   min-height:100vh;
}

.footer {
  margin-top: 6rem;
}

</style>

<script>
export default {
  computed: {
    menu () {
      return this.$store.state.menu
    },
    schema () {
      return this.$store.state.schema
    },
    brandHref () {
      return '/' + this.$store.state.schema
    }
  },
}
</script>