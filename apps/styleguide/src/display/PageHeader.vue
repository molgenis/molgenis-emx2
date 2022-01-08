<template>
  <div>
    <div class="row">
      <div class="col-9">
        <h1>{{ title }}</h1>
      </div>

      <div class="col-3 d-flex flex-row-reverse">
        <img
          v-if="logoUrl"
          class="justify-content-center align-self-center"
          :src="logoUrl"
          alt="page logo"
        />
      </div>
    </div>
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
    logoUrl: {
      type: String,
      required: false,
    },
  },
};
</script>

<docs>
Example (label only)

```
<template>
  <page-header title="My Title"></page-header>
</template>


```

Example (label and logo)

```
<template>
  <page-header style="width: 300px; border: 1xp solid blue;" title="Molgenis" logoUrl="https://www.molgenis.org/assets/img/logo_blue.png"></page-header>
</template>

````

</docs>
