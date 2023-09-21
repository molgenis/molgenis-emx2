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
import { postQuery } from "./utils/utils";

import { Molgenis } from "molgenis-components";
import { LoadingScreen, MessageBox } from "molgenis-viz";
import AppFooter from "./components/AppFooter.vue";

const session = ref(null);
const page = ref(null);

let loading = ref(false);
let errorMessage = ref(false);
let userData = ref({});

async function getUserData () {
  const result = await postQuery(
    'api/graphql',
    `{
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
  )
  
  return result.data.Users
}

async function loadData() {
  const userResult = await getUserData();
  const users = userResult[0];
  users.orgId = users.organisation.providerInformation[0].providerIdentifier;
  users.orgName = users.organisation.name;
  users.orgImageUrl = users.organisation.imageUrl;
  delete users.organisation;
  userData.value = users;
}

onMounted(() => {
  try {
    loadData();
  } catch (error) {
    errorMessage.value = error;
  }
});

</script>
