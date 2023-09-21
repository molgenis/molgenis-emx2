<template>
  <div>
    <div class="row">
      <div :class="'col-' + nTitleColumns">
        <h1>{{ title }}</h1>
      </div>

      <div v-if="logoUrl" class="col-3 d-flex flex-row-reverse">
        <img
          class="justify-content-center align-self-center"
          :src="logoUrl"
          alt="page logo"
        />
      </div>
    </div>
    <h3 v-if="subTitle">
      <small class="text-muted">
        <template v-if="subTitleLink && isVueRouterLink">
          <router-link v-bind="subTitleLink">{{ subTitle }}</router-link>
        </template>
        <template v-else-if="subTitleLink && !isVueRouterLink"
          ><a :href="subTitleLink">{{ subTitle }}</a></template
        >
        <template v-else>{{ subTitle }}</template>
      </small>
    </h3>
  </div>
</template>

<style scoped>
img {
  /* same as bootstrap heading */
  height: 2.5rem;
}
</style>

<script>
export default {
  name: "PageHeader",
  props: {
    title: {
      type: String,
      required: true,
    },
    subTitle: {
      type: String,
      required: false,
    },
    subTitleLink: {
      type: [String, Object],
      required: false,
    },
    logoUrl: {
      type: String,
      required: false,
    },
  },
  computed: {
    nTitleColumns() {
      return this.logoUrl ? 9 : 12;
    },
    isVueRouterLink() {
      return (
        (this.subTitleLink && typeof this.subTitleLink === "object") ||
        (typeof this.subTitleLink === "string" &&
          !(
            this.subTitleLink.startsWith("http://") ||
            this.subTitleLink.startsWith("https://")
          ))
      );
    },
  },
};
</script>

<docs>
<template>
  <div>
    <label class="font-italic">basic header with a title</label>
    <demo-item>
      <page-header title="My title"></page-header>
    </demo-item>

    <label class="font-italic">header with title and subtitle</label>
    <demo-item>
      <page-header title="My title" subTitle="and subtitle"></page-header>
    </demo-item>

    <label class="font-italic">Header with subtitle and logo</label>
    <demo-item>
      <page-header
          title="My title"
          subTitle="and subtitle"
          logoUrl="https://avatars.githubusercontent.com/u/1688158?s=200&v=4"
      ></page-header>
    </demo-item>
  </div>
</template>
</docs>
