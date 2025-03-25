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
  methods: {
    parseContent() {
      if (this.contents) {
        const parser = new DOMParser();

        if (this.contents.html) {
          const doc = parser.parseFromString(this.contents.html, "text/html");
          Array.from(doc.body.children).forEach((elem) => {
            this.$refs.pageContents.appendChild(elem);
          });
        }

        if (this.contents.css) {
          const styleElem = document.createElement("style");
          styleElem.textContent = this.contents.css;
          this.$refs.pageContents.appendChild(styleElem);
        }

        if (this.contents.javascript) {
          const scriptElem = document.createElement("script");
          scriptElem.setAttribute("type", "text/javascript");
          scriptElem.textContent = this.contents.javascript;
          this.$refs.pageContents.appendChild(scriptElem);
        }
      }
    },
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
      return null;
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
    contents(newContent, oldContent) {
      if (newContent && oldContent === null) {
        this.parseContent();
      }
    },
  },
  mounted() {
    this.parseContent();
  },
};
</script>
