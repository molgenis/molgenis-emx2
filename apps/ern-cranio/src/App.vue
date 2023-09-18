<template>
  <Molgenis id="__top" v-model="session">
    <router-view
      :session="session"
      :page="page"
      :user="user"
      :organization="org"
    />
    <AppFooter :organization="org" />
  </Molgenis>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { Molgenis } from "molgenis-components";
import AppFooter from "./components/AppFooter.vue";
import { fetchData } from "../../molgenis-viz/src/utils/utils";

const session = ref(null);
const page = ref(null);

const org = ref({
  id: "umcg",
  name: "UMCG",
  label: "University Medical Centre Groningen",
});


const userData = ref({})
const user = ref("David");
const query = `{
  Users (
    filter: {
      name: { equals: "${user.value}" }
    }
  ) {
    name
    parent {
      name
      label
    }
  }
}`

function flattenNestedObject(obj, targetKey) {
  Object.keys(obj).map(key => {
    if (key === targetKey) {
      Object.keys(obj[targetKey]).map(subKey => {
        Object.assign(obj, { [`${targetKey}_${subKey}`]: obj[key][subKey] })
      })
    }
  })
}

onMounted(() => {
  Promise.resolve(fetchData('/api/graphql', query))
  .then(response => {
    const userAccount = response.data.Users[0];
    flattenNestedObject(userAccount, 'parent')
    userData.value = userAccount
  })
})

</script>
