<template>
  <div class="col-sm-4 py-2">
    <div class="card h-100 overflow-auto">
      <span>
        <h4 class="card-title">
          <RouterLink
            :to="{
              name: 'release',
              params: {
                resourceAcronym: release.resource.acronym,
                version: release.version,
              },
            }"
          >
            <small>
              {{ release.resource.acronym }} {{ release.version }}
            </small>
          </RouterLink>
        </h4>
      </span>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    release: Object,
  },
};
</script>
