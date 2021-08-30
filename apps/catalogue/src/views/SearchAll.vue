<template>
  <div>
    <h1>Search All</h1>
    <main>
      <form v-on:submit.prevent="onSubmit">
        <input type="text" v-model="query" />
        <button type="submit">Search</button>
      </form>
    </main>
  </div>
</template>

<script>
export default {
  name: "SearchAll",
  data() {
    return {
      query: "",
    };
  },
  methods: {
    onSubmit() {
      console.log(this.query);
      this.$router.push({
        name: "SearchResults",
        query: { q: this.query },
      });
    },
  },
};
</script>

<style></style>
