<template>
  <form v-on:submit.prevent="onSubmit" class="form-horizontal">
    <div class="input-group">
      <input
        type="text"
        class="form-control form-control-lg"
        v-model="query"
        placeholder="Search"
      />
      <button type="submit" class="btn btn-primary btn-lg">Search</button>
    </div>
  </form>
</template>

<script>
export default {
  name: "SearchResource",
  props: {
    resourceType: {
      type: String, // one of Resource
      default: () => "institutions",
    },
  },
  data() {
    return {
      query: "",
    };
  },
  methods: {
    onSubmit() {
      this.$router.push({
        name: this.resourceType,
        query: { q: this.query },
      });
    },
  },
};
</script>

<style></style>
