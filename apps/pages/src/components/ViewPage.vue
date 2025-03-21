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
    contents() {
      const html = this.session.settings["page." + this.page].html;
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, "text/html");

      Array.from(doc.body.children).forEach((el) => {
        if (el.tagName === "SCRIPT") {
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
        } else if (el.tagName === "STYLE") {
          const styleEl = document.createElement("style");
          styleEl.textContent = el.textContent;
          this.$refs.pageContents.appendChild(styleEl);
        } else {
          this.$refs.pageContents.appendChild(el);
        }
      });
    },
  },
};
</script>
