<script setup lang="ts">
import { useRouter } from "#app/composables";
import { navigateTo, useRoute } from "#app/composables/router";
import { ref } from "vue";
import { useSession } from "../composables/useSession";

const route = useRoute();
const router = useRouter();

const username = ref("");
const password = ref("");

const error = ref("");
const loading = ref(false);

async function signin() {
  if (!username.value || !password.value) {
    error.value = "Email and password should be filled in";
  } else {
    error.value = "";
    loading.value = true;
    const signinResp = await $fetch("/api/graphql", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        query: `mutation{signin(email: "${username.value}", password: "${password.value}"){status,message}}`,
      }),
    }).catch((error) => {
      error.value = "internal server graphqlError" + error;
      loading.value = false;
    });

    loading.value = false;

    if (signinResp.data.signin.status === "SUCCESS") {
      console.log(signinResp.data.signin);
      (await useSession()).reload();
      route.redirectedFrom || router.getRoutes().length
        ? router.back()
        : navigateTo({ path: "/" });
    } else {
      console.log(signinResp.data.signin.message);
      error.value = signinResp.data.signin.message;
    }
  }
}
</script>
<template>
  <Container class="flex flex-col items-center">
    <PageHeader title="Login" />

    <ContentBlock class="w-6/12" title="">
      <form class="flex flex-col gap-4" v-on:submit.prevent="signin">
        <InputString
          id="username"
          placeholder="Username"
          :required="true"
          v-model="username"
          autofocus
          autocomplete="username"
        />
        <InputString
          id="password"
          type="password"
          autocomplete="current-password"
          placeholder="Password"
          :required="true"
          v-model="password"
        />
        <Button type="primary" size="medium">Sign in</Button>
        <div class="text-red-500">
          {{ error }}
        </div>
      </form>
    </ContentBlock>
  </Container>
</template>
