<template>
  <div class="container">
    <h1>Welcome at the European cohort network catalogue.</h1>
    <p>Choose a network below to explore.</p>
    <div class="card-deck">
      <div class="card p-4">
        <img
          src="https://lifecycle-project.eu/wp-content/uploads/2020/03/euchild_logo-e1583949892302.jpg"
          class="card-img-top"
          style="max-width: 300px"
          alt="..."
        />
        <div class="card-body">
          <h5 class="card-title">EUChildNetwork</h5>
          <p>
            A Europe-wide network of cohort studies started in early life Cohort
            studies started from pregnancy or childhood give the unique
            opportunity to relate early-life stressors with variation in
            development, health and disease throughout the life course.
          </p>
          <a href="#" class="btn btn-primary disabled">View cohorts</a>
          <RouterLink
            class="btn btn-primary ml-2"
            :to="{ name: 'NetworkVariables', params: { network: 'LifeCycle' } }"
            >View variables
          </RouterLink>
        </div>
      </div>
      <div class="card p-4">
        <img
          src="https://h2020-remedia.eu/wp-content/themes/lbs/images/ehen-European-Human-Exposome-Network-medium-logo.png"
          class="card-img-top"
          alt="..."
        />
        <div class="card-body">
          <h5 class="card-title">European Human Exposome Network</h5>
          <p>
            The European Human Exposome Network brings together research
            projects studying the impact of environmental exposure on human
            health. These projects address issues such as exposures to air
            quality, noise, chemicals, urbanisation etc. and health impacts.
          </p>
          <a href="#" class="btn btn-primary disabled">View cohorts</a>
          <a href="#" class="btn btn-primary ml-2 disabled">View variables</a>
        </div>
      </div>
    </div>
    <p>
      This catalogue was made possible by EUCAN-connect, EU-LifeCycle,
      EU-Athlete and EU-LongITools, in collaboration with members of European
      Human Exposome Network.
    </p>
  </div>
</template>

<style>
.card-img-top {
  width: 100%;
  object-fit: contain;
}
</style>
