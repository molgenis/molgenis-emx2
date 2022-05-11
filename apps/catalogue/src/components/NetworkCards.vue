<template>
  <div class="card p-4">
    <img
      :src="
        network.logo.url
          ? network.logo.url
          : 'data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=='
      "
      class="card-img-top"
      style="height: 4em"
      alt="..."
    />
    <div class="card-body">
      <h5 class="card-title">{{ network.pid }}</h5>
      <p>
        A Europe-wide network of cohort studies started in early life Cohort
        studies started from pregnancy or childhood give the unique opportunity
        to relate early-life stressors with variation in development, health and
        disease throughout the life course.
      </p>
      <p>{{ network.fundingStatement }}</p>
      <div class="card-footer bg-white mt-auto">
        <RouterLink
          class="btn btn-primary ml-2"
          :to="{ name: 'NetworkDetails', params: { network: network.pid } }"
        >
          description
        </RouterLink>
        <RouterLink
          class="btn btn-primary ml-2"
          :to="{ name: 'NetworkCohorts', params: { network: network.pid } }"
        >
          cohorts
        </RouterLink>
        <RouterLink
          class="btn btn-primary ml-2"
          :to="{ name: 'NetworkVariables', params: { network: network.pid } }"
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

<style>
.card-footer {
  border: none;
  position: absolute;
  bottom: 0;
}
</style>
