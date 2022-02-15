<template>
  <div>
    <div class="container">
      <h1>Schemas</h1>
      <div class="list-group">
        <nuxt-link
          v-for="schema in schemas"
          :key="schema.name"
          :to="'/' + schema.name + '/ssr-catalogue'"
          class="list-group-item list-group-item-action"
        >
          {{ schema.name }}
        </nuxt-link>
      </div>
    </div>
  </div>
</template>


<script>
const query = "{ Schemas { name description } }";
export default {
  async asyncData({ $axios, store }) {
    store.dispatch("fetchSession");

    const resp = await $axios.post("/apps/graphql", { query }).catch((e) => {
      console.log("failed to fetch schemas");
      console.log(e);
    });

    return resp ? { schemas: resp.data.data.Schemas } : null;
  },
};
</script>
