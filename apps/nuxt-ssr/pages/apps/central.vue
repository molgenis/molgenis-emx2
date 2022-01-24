<template>
<div>
  <Menu :menu="menu"/>
  <div class="container">
    <h1>Schemas</h1>
    
    <div class="list-group">
      <nuxt-link v-for="schema in schemas" :key="schema.name" :to="'/' + schema.name + '/ssr-catalogue'" class="list-group-item list-group-item-action">
        {{schema.name}}
      </nuxt-link>
    </div>
  </div>
</div>
</template>


<script>
import Menu from '../../components/Menu.vue'

  export default {
    components: { Menu },
    async asyncData({ params, $axios, store }) {
      store.dispatch('fetchSession')
      const query = '{Schemas{name description}}'
      const resp = await $axios({
      url: "apps/central/graphql",
      method: "post",
      data: { query }
    }).catch(e => {
      console.error(e)
    });
    return { schemas: resp.data.data.Schemas }
    },
    computed: {
      menu () {
        return this.$store.state.menu
      }
    }
  }
</script>
