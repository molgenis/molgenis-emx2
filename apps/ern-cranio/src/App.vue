<template>
  <Molgenis id="__top" v-model="session">
    <p>{{ schema }}</p>
    <p>{{ currentSchemaApi }}</p>
    <p>{{ primarySchemaApi }}</p>
    {{ currentProvider }}
    
    <!-- <LoadingScreen v-if="loading && !error"/>
    <div class="message-box-container" v-else-if="!loading && error">
      <MessageBox type="error">
        <p>Error: unable to retrieve data. {{ error }}</p>
      </MessageBox>
    </div>
    <div v-else>
      <router-view
        :session="session"
        :page="page"
        :providerId="currentProvider.id"
        :providerName="currentProvider.name"
        :providerImageUrl="currentProvider.imageUrl"
      />
      <AppFooter
        :providerId="currentProvider.id"
        :providerName="currentProvider.name"
        :providerImageUrl="currentProvider.imageUrl"
      />
    </div> -->
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


let loading = ref(true);
let error = ref(false);
let currentProvider = ref({});

const primarySchema = ref("CranioStats");
const primarySchemaApi = ref(`/${primarySchema.value}/api/graphql`);
let currentSchema = ref(null);
let currentSchemaApi = ref(null);
let schema = ref(null);

function setSchema() {
  const path = window.location.pathname
  if (path !== "/") {
    const schema = path.split("/").filter(value => value != "");
    currentSchema.value = `/${schema}/`
    currentSchemaApi.value = `/${currentGraphqlApi.value}/api/graphql`
  } else {
    currentSchema.value = "/";
    currentSchemaApi.value = "/api/graphql";
  }
}

async function getCurrentProvider(schema) {
  const result = await postQuery(
    primarySchemaApi.value,
    `{
        Organisations (
          filter: {
            providerInformation: {
              providerIdentifier: {
                equals: ${schema}
              }
            }
          }
        ) {
          name
        }
      }`
  );
  return result.data.Organisations;
}

async function loadData() {
  const result = await getCurrentProvider(currentSchema.value);
  const provider = result[0];
  currentProvider.value = {
    id: provider.providerInformation[0].providerIdentifier,
    name:  provider.name,
    imageUrl: provider.imageUrl
  }
}

onMounted(() => {
  setSchema();

  try {
    loadData();
  } catch (err) {
    error.value = err;
  } finally {
    loading.value = false;
  }
});
</script>
