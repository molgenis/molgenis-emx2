<template>
  <div>
    <router-link v-if="canEdit" :to="'/' + page + '/edit'"
      >edit page
    </router-link>
    <div v-html="contents"></div>
  </div>
</template>

<script>
export default {
  props: {
    page: String,
    session: Object,
  },
  computed: {
    contents() {
      if (
        this.session &&
        this.session.settings &&
        this.session.settings["page." + this.page]
      ) {
        return this.session.settings["page." + this.page];
      }
      return "Page not found";
    },
    canEdit() {
      return (
        this.session &&
        (this.session.email == "admin" ||
          (this.session.roles && this.session.roles.includes("Manager")))
      );
    },
  },
};
</script>
