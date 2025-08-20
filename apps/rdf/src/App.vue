<template>
  <Molgenis v-model="session" @error="error = $event">
    <h3>RDF</h3>
    <p>
      For information about the RDF in EMX2, please view the docs about the
      <a
        href="https://molgenis.github.io/molgenis-emx2/#/molgenis/dev_rdf"
        target="_blank"
        >RDF API</a
      >
      and the
      <a
        href="https://molgenis.github.io/molgenis-emx2/#/molgenis/semantics"
        target="_blank"
        >semantics</a
      >
      field.
    </p>
    <div v-if="session" class="card">
      <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs">
          <li class="nav-item" v-for="item in navTabs" :key="item.id">
            <router-link
              class="nav-link"
              :class="{ active: selected == item.id }"
              :to="item.id"
              >{{ item.name }}
            </router-link>
          </li>
        </ul>
      </div>
      <div class="card-body">
        <router-view />
      </div>
    </div>
    <MessageError v-else-if="error">
      You have to be logged in with right permissions to see this menu
    </MessageError>
    <Spinner v-else />
  </Molgenis>
</template>

<script setup lang="ts">
import {
  MessageError,
  MessageWarning,
  Molgenis,
  Spinner,
} from "molgenis-components";
import { ref, computed } from "vue";
import { useRouter } from "vue-router";

const session = ref(null);
const error = ref(null);

const navTabs = ref([
  { id: "sparql", name: "SPARQL" },
  { id: "shacl", name: "SHACL" },
]);

const selected = computed(() => {
  return useRouter().currentRoute.value.name;
});
</script>
