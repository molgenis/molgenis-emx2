<template>
  <form v-on:submit.prevent="onSubmit" class="form-horizontal">
    <div class="input-group">
      <input
        type="text"
        class="form-control form-control-lg"
        v-model="query"
        :placeholder="placeholder" />
      <div class="input-group-append">
        <button type="submit" class="btn btn-primary btn-lg">Search</button>
      </div>
    </div>
  </form>
</template>

<script>
export default {
  name: "SearchResource",
  props: {
    resourceType: {
      type: String, // one of Resource
      default: () => "Institutions",
    },
    placeholder: {
      type: String,
      required: false,
      default: () => "Search",
    },
  },
  data() {
    return {
      query: "",
    };
  },
  methods: {
    onSubmit() {
      const route = { name: this.resourceType };
      if (this.query) {
        route.query = { q: this.query };
      }
      this.$router.push(route);
    },
  },
};
</script>
