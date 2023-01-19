<template>
  <div class="card p-4 w-100">
    <div class="image-container">
      <img
        :src="
          network.logo.url
            ? network.logo.url
            : 'data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=='
        "
        alt="..."
      />
    </div>
    <div class="card-body">
      <h5 class="card-title">{{ network.id }}</h5>
      <p v-if="network.description && network.description.trim().length > 0">
        {{ network.description.substring(0, 500) }}
        <span v-if="network.description.length > 500">...</span>
      </p>
      <p v-else>No description available.</p>
      <p v-if="network.fundingStatement">
        {{ network.fundingStatement.substring(0, 500) }}
        <span v-if="network.fundingStatement.length > 500">...</span>
      </p>
      <div class="card-footer bg-white mt-auto">
        <RouterLink
          class="btn btn-primary ml-2"
          :to="{ name: 'NetworkDetails', params: { network: network.id } }"
        >
          description
        </RouterLink>
        <RouterLink
          class="btn btn-primary ml-2"
          :to="{ name: 'NetworkCohorts', params: { network: network.id } }"
        >
          cohorts
        </RouterLink>
        <RouterLink
          class="btn btn-primary ml-2"
          :to="{ name: 'NetworkVariables', params: { network: network.id } }"
        >
          variables
        </RouterLink>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    network: Object,
  },
};
</script>

<style scoped>
.card-footer {
  border: none;
  position: absolute;
  bottom: 0;
}
.image-container {
  height: 4em;
  display: flex;
  justify-content: center;
}
.image-container > img {
  height: 100%;
}
</style>
