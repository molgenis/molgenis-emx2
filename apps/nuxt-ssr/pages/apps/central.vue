<template>
  <div>
    <div class="container">
      <h1>Schemas</h1>
      <div class="list-group">
        <a
          v-for="schema in schemas"
          :key="schema.name"
          class="list-group-item list-group-item-action"
          :href="'/' + schema.name + '/ssr-catalogue'"
          >{{ schema.name }}</a
        >
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
