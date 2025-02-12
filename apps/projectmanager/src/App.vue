<template>
  <Molgenis id="__top" v-model="session">
    <router-view :session="session" :page="page" :schema="schemaName" />
  </Molgenis>
</template>

<script setup lang="ts">
import { Molgenis } from "molgenis-components";
import { request } from "graphql-request";
import { isEmpty } from "metadata-utils";
import type { ISetting } from "metadata-utils";
import { ref, onBeforeMount } from "vue";

import schemaNameQuery from "./gql/schemaName";

const setting: ISetting = {
  key: "test",
  value: "test",
};

console.log("is setting empty : ", isEmpty(setting));

const session = ref(null);
const page = ref(null);
const schemaName = ref<string | null>(null);
const graphqlError = ref<Error | null>(null);
const loading = ref<boolean>(true);

function getSchemaName() {
  request("graphql", schemaNameQuery)
    .then((response: object) => {
      schemaName.value = response._schema.name;
    })
    .catch((error: Error) => {
      graphqlError.value = error;
      loading.value = false;
    });
}

onBeforeMount(() => {
  getSchemaName();
});
</script>
