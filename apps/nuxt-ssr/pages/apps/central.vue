<template>
<div>
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
  export default {
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
    }
  }
</script>
