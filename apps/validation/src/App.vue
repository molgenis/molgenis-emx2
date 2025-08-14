<template>
  <Molgenis v-model="session" @error="error = $event">
    <h3>Validation</h3>

    <div v-if="session" class="card">
      <div class="card-header">
        <ul class="nav nav-tabs card-header-tabs">
          <li class="nav-item" v-for="item in navTabs" :key="item.id">
            <router-link
                class="nav-link"
                :class="{active: selected == item.id}"
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
import { useRouter} from "vue-router";

const session = ref(null);
const error = ref(null);

const navTabs = ref([
  { id: "shacl", name: "SHACL" },
  { id: "table", name: "Table" },
]);

const selected = computed(() => {return useRouter().currentRoute.value.name})
</script>
