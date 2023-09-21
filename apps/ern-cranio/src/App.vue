<template>
  <Molgenis id="__top" v-model="session">
    <LoadingScreen v-if="loading && !errorMessage" />
    <div class="message-box-container" v-else-if="!loading && errorMessage">
      <MessageBox type="error">
        <p>Unable to display page.</p>
        {{ errorMessage }}
      </MessageBox>
    </div>
    <div v-else-if="!loading && !errorMessage && userData.orgId">
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
  </Molgenis>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { Molgenis } from "molgenis-components";
import { LoadingScreen, MessageBox } from "molgenis-viz";
import AppFooter from "./components/AppFooter.vue";
import { fetchData } from "../../molgenis-viz/src/utils/utils";

const session = ref(null);
const page = ref(null);

let loading = ref(false);
let errorMessage = ref(false);
let userData = ref({});

// for test purposes only
const queryUsers = `{
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

function setBaseApiUrl() {
  const host = window.location.hostname || "";
  if (host !== "localhost") {
    const path = window.location.pathname.split("/").filter(value => value !== "")[0];
    return `/${path}/api/graphql`
  }
  return '/api/graphql';
}

let apiEndpoint = ref(setBaseApiUrl());


onMounted(() => {
  Promise.resolve(fetchData(apiEndpoint.value, queryUsers))
  .then(response => {
    const data = response.data.Users[0];
    data.orgId = data.organisation.providerInformation[0].providerIdentifier;
    data.orgName = data.organisation.name;
    data.orgImageUrl = data.organisation.imageUrl;
    delete data.organisation;
    userData.value = data;
  })
  .then(() => {
    if (Object.keys(userData.value).length === 4) {
      loading.value = false;
    } else {
      throw new Error('data not defined', userData);
    }
  }).catch(error => {
    errorMessage.value = error;
    throw new Error(error);
  })
})

</script>
