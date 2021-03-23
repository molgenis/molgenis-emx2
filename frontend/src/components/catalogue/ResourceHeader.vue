<template>
  <div>
    <div class="p-2 mb-2" :class="headerCss">
      <h6>
        <RouterLink :class="headerCss" to="/">
          home
        </RouterLink>
        /
        <RouterLink :class="headerCss" :to="'/list/' + tableName">
          {{ tableName }}
        </RouterLink>
        /
      </h6>
    </div>
    <div class="row">
      <div class="col">
        <h4>
          <span
            v-if="resource.type"
            class="badge badge-pill float-right"
            :class="headerCss"
          >
            {{ resource.type.map((t) => t.name).join(",") }}
          </span>
        </h4>
        <h1>
          {{ resource.name }} <sup>({{ resource.acronym }})</sup>
        </h1>

        <a v-if="resource.homepage" :href="resource.homepage">
          {{ resource.homepage }}
        </a>
        <p v-else>
          Homepage: N/A
        </p>
        <p v-if="resource.description">
          {{ resource.description }}
        </p>
        <p v-else>
          Description: N/A
        </p>
      </div>
      <div v-if="resource.logo" class="col-2">
        <img class="w-100" :src="resource.logo.url">
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    headerCss: String,
    resource: Object,
    tableName: String,
  },
}
</script>
