<template>
  <Molgenis id="__top" v-model="session">
    <div class="hpc-app-shell">
      <div class="hpc-content-wrap">
        <header class="hpc-app-header">
          <div class="hpc-app-header-top">
            <div>
              <p class="hpc-page-kicker">Compute Operations</p>
              <h1 class="hpc-page-title">HPC Dashboard</h1>
              <p class="hpc-page-subtitle">
                Monitor worker health, review job lifecycles, and inspect artifacts from a single
                operational workspace.
              </p>
            </div>
            <nav v-if="signedIn" class="hpc-nav-pills" aria-label="HPC navigation">
              <router-link
                to="/"
                class="hpc-nav-pill"
                :class="{ active: $route.path === '/' || $route.path.startsWith('/jobs') }"
              >
                Jobs
              </router-link>
              <router-link
                to="/workers"
                class="hpc-nav-pill"
                :class="{ active: $route.path === '/workers' }"
              >
                Workers
              </router-link>
              <router-link
                to="/artifacts"
                class="hpc-nav-pill"
                :class="{ active: $route.path.startsWith('/artifacts') }"
              >
                Artifacts
              </router-link>
            </nav>
          </div>

          <div class="hpc-app-header-meta">
            <span class="hpc-meta-chip">
              <strong>Auth</strong>
              {{ signedIn ? "Signed in" : "Anonymous" }}
            </span>
            <span v-if="signedIn" class="hpc-meta-chip">
              <strong>User</strong>
              {{ session?.email || session?.name || "unknown" }}
            </span>
            <span class="hpc-meta-chip">
              <strong>Refresh</strong>
              Lists auto-refresh while open
            </span>
          </div>
        </header>

        <div v-if="!signedIn" class="alert alert-warning hpc-auth-warning hpc-surface">
          Please sign in using the menu above to access the HPC dashboard.
        </div>
        <router-view v-else :session="session" />
      </div>
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
