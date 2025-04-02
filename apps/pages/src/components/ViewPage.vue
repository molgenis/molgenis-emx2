<template>
  <div>
    <router-link v-if="canEdit" :to="'/' + page + '/edit'">
      edit page
    </router-link>
    <div ref="pageContents"></div>
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
  watch: {
    contents(htmlString) {
      const parser = new DOMParser();
      const doc = parser.parseFromString(htmlString, "text/html");

      /** Loop over the just parsed html items, and add them */
      Array.from(doc.body.children).forEach((el) => {
        if (el.tagName !== "SCRIPT") {
          this.$refs.pageContents.appendChild(el);
        } else {
          /** Script tags need a special treatment, else they will not execute. **/
          const scriptEl = document.createElement("script");
          if (el.src) {
            /** If we have an external script. */
            scriptEl.src = el.src;
          } else {
            /** Regular inline script */
            scriptEl.textContent = el.textContent;
          }
          this.$refs.pageContents.appendChild(scriptEl);
        }
      });
    },
  },
};
</script>
