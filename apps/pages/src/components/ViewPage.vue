<template>
  <div>
    <router-link v-if="canEdit" :to="'/' + page + '/edit'">
      edit page
    </router-link>
    <MessageError v-if="graphqlError">{{ graphqlError }}</MessageError>
    <div ref="pageContents"></div>
  </div>
</template>

<script>
import { MessageError } from "molgenis-components";
import { getPageSetting } from "../utils/getPageSetting";
import { generateHtmlPreview } from "../utils/generateHtmlPreview";
import { newPageContentObject } from "../utils/newPageContentObject";

export default {
  props: {
    page: String,
    session: Object,
  },
  components: {
    MessageError,
  },
  data() {
    return {
      graphqlError: null,
    };
  },
  computed: {
    pageSettingKey() {
      return "page." + this.page;
    },
    canEdit() {
      return (
        this.session &&
        (this.session.admin ||
          (this.session.roles && this.session.roles.includes("Manager")))
      );
    },
  },
  mounted() {
    Promise.resolve(getPageSetting(this.pageSettingKey))
      .then((data) => {
        if (
          data &&
          (Object.keys(data).includes("html") ||
            Object.keys(data).includes("css") ||
            Object.keys(data).includes("javascript"))
        ) {
          this.content = data;
        } else if (data && typeof data === "string") {
          this.content = data;
        } else {
          this.content = newPageContentObject();
        }
      })
      .then(() => generateHtmlPreview(this, this.content, "pageContents"))
      .catch((err) => (this.graphqlError = err));
  },
};
</script>
