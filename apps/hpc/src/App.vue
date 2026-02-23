<template>
  <Molgenis id="__top" v-model="session">
    <div class="container-fluid py-3">
      <div class="d-flex justify-content-between align-items-center mb-3">
        <h4 class="mb-0">HPC Dashboard</h4>
        <div v-if="signedIn">
          <ul class="nav nav-tabs mb-0">
            <li class="nav-item">
              <router-link
                to="/"
                class="nav-link"
                :class="{ active: $route.path === '/' || $route.path.startsWith('/jobs') }"
              >
                Jobs
              </router-link>
            </li>
            <li class="nav-item">
              <router-link
                to="/workers"
                class="nav-link"
                :class="{ active: $route.path === '/workers' }"
              >
                Workers
              </router-link>
            </li>
            <li class="nav-item">
              <router-link
                to="/artifacts"
                class="nav-link"
                :class="{ active: $route.path.startsWith('/artifacts') }"
              >
                Artifacts
              </router-link>
            </li>
          </ul>
        </div>
      </div>
      <div v-if="!signedIn" class="alert alert-warning">
        Please sign in using the menu above to access the HPC dashboard.
      </div>
      <router-view v-else :session="session" />
    </div>
  </Molgenis>
</template>

<script setup>
import { Molgenis } from "molgenis-components";
import { ref, computed } from "vue";

const session = ref(null);
const signedIn = computed(
  () => session.value?.email && session.value.email !== "anonymous"
);
</script>
