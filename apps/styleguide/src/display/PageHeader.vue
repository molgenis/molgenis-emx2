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
    <h3>
      <small class="text-muted">{{ subTitle }}</small>
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
    logoUrl: {
      type: String,
      required: false,
    },
  },
  computed: {
    nTitleColumns() {
      return this.logoUrl ? 9 : 12;
    },
  },
};
</script>

<docs>
Example (title only)

```
<template>
  <page-header title="My Title"></page-header>
</template>


```

Example (title and logo)

```
<template>
  <page-header style="width: 300px; border: 1xp solid blue;" title="Molgenis" logoUrl="https://www.molgenis.org/assets/img/logo_blue.png"></page-header>
</template>

````

Example (title and subTitle)

```
<template>
  <page-header title="Molgenis" subTitle="All your data are belong to us." ></page-header>
</template>
````

</docs>
