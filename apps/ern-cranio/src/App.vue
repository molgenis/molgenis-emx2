<template>
  <Molgenis id="__top" v-model="session">
    <div v-if="userData.orgId">
      <router-view
      :session="session"
      :page="page"
      :userName="userData.name"
      :orgId="userData.orgId"
      :orgName="userData.orgName"
      :orgImageUrl="userData.orgImageUrl"
      />
      <AppFooter
        :userName="userData.name"
        :orgId="userData.orgId"
        :orgName="userData.orgName"
        :orgImageUrl="userData.orgImageUrl"
      />
    </div>
    <LoadingScreen message="Please wait..." v-else/>
  </Molgenis>
</template>

<script setup>
import { ref, onBeforeMount } from "vue";
import { Molgenis } from "molgenis-components";
import { LoadingScreen } from "molgenis-viz";
import AppFooter from "./components/AppFooter.vue";
import { fetchData } from "../../molgenis-viz/src/utils/utils";

const session = ref(null);
const page = ref(null);

let loading = ref(false);
let userData = ref({});
let user = ref("David");

const query = `{
  Users (
    filter: {
      definition: { equals: "live" }
    }
  ) {
    name
    organisation {
      name
      providerInformation {
        providerIdentifier
      }
      imageUrl
    }
  }
}`

onBeforeMount(() => {
  Promise.resolve(fetchData('/api/graphql', query))
  .then(response => {
    const data = response.data.Users[0];
    data.orgId = data.organisation.providerInformation[0].providerIdentifier;
    data.orgName = data.organisation.name
    data.orgImageUrl = data.organisation.imageUrl
    delete data.organisation
    userData.value = data;
  })
  .then(() => {
    if (Object.keys(userData.value).length === 4) {
      loading.value = false;
    } else {
      throw new Error('data not defined', userData)
    }
  }).catch(error => {
    throw new Error(error);
  })
})

</script>
