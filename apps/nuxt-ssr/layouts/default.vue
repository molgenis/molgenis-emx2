<template>
<div class="wrapper d-flex flex-column">
  <Menu :brandHref=brandHref :menu="menu"/>
  <BreadCrumb v-if="isBreadCrumbShown" :crumbs="crumbs" />
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
import { BreadCrumb } from "molgenis-components";
export default {
  components: { BreadCrumb },
  computed: {
    menu () {
      return this.$store.state.menu
    },
    schema () {
      return this.$store.state.schema
    },
    brandHref () {
      return '/' + this.$store.state.schema
    },
    crumbs () {
      const sections = this.$route.path.split("/").filter(section => section !== "") 

      // given a path section walk the path (building the url) until section is found 
      const buildUrl = (section) => {
        return sections.reduce((url, current) => {
          return url.split("/").pop() !== section ? url  + "/" + current : url
        })
      }

      return sections.reduce((accum, section) => {
        // add "/" to make absolute path
        const routeUrl = "/" + buildUrl(section)
        accum[section] = routeUrl
        return accum
      }, {})
    },
    isBreadCrumbShown () {
      return this.$route.path !== "/apps/central/"
    }
  },
}
</script>