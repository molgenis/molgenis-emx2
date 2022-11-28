<template>
  <div>
    <div class="p-2 mb-2" :class="headerCss" v-if="resource">
      {{ tableName }}
    </div>
    <div class="row">
      <div class="col">
        <h4></h4>
        <h1>
          {{ resource.name }}
        </h1>
        <a v-if="resource.homepage" :href="resource.homepage">
          {{ resource.homepage }}
        </a>
        <p v-if="resource.description">{{ resource.description }}</p>
        <p v-else>Description: N/A</p>
      </div>
      <div class="col-2" v-if="resource.logo">
        <img class="w-100" :src="resource.logo.url" />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    resource: Object,
    tableName: String,
    headerCss: String,
  },
};
</script>
