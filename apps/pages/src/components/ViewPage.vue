<template>
  <div>
    <router-link v-if="canEdit" :to="'/' + page + '/edit'">
      edit page
    </router-link>
    <ModularPage
      v-if="contents?.version === 2"
      :editMode="false"
      :content="contents"
      :page="page"
    ></ModularPage>
    <div v-else v-html="contents"></div>
  </div>
</template>

<script>
import ModularPage from "./ModularPage.vue";

export default {
  components: {
    ModularPage,
  },
  props: {
    page: String,
    session: Object,
  },
  computed: {
    contents() {
      if (
        this?.session &&
        this?.session?.settings &&
        this?.session?.settings["page." + this.page]
      ) {
        return this?.session?.settings["page." + this.page];
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
