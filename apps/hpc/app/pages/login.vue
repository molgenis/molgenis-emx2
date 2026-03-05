<script setup lang="ts">
import { navigateTo } from "#app/composables/router";
import { ref } from "vue";
import { useSession } from "../../../tailwind-components/app/composables/useSession";
import PageHeader from "../../../tailwind-components/app/components/PageHeader.vue";
import Container from "../../../tailwind-components/app/components/Container.vue";
import Button from "../../../tailwind-components/app/components/Button.vue";
import InputString from "../../../tailwind-components/app/components/input/String.vue";
import ContentBlock from "../../../tailwind-components/app/components/content/ContentBlock.vue";

const username = ref("");
const password = ref("");
const error = ref("");
const loading = ref(false);

type SigninResponse = {
  data: {
    signin: {
      status: "SUCCESS" | string;
      message: string;
    };
  };
};

async function signin() {
  if (!username.value || !password.value) {
    error.value = "Email and password should be filled in";
    return;
  }

  error.value = "";
  loading.value = true;

  const signinResp = await $fetch<SigninResponse>("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      query: `mutation{signin(email: "${username.value}", password: "${password.value}"){status,message}}`,
    }),
  }).catch((err) => {
    error.value = "Sign in failed: " + err;
    loading.value = false;
  });

  loading.value = false;

  if (signinResp?.data.signin.status === "SUCCESS") {
    await (await useSession()).reload();
    navigateTo({ path: "/", replace: true });
  } else {
    error.value = signinResp?.data.signin.message || "Unknown error";
  }
}
</script>

<template>
  <Container class="flex flex-col items-center">
    <PageHeader title="Login" />

    <ContentBlock class="w-6/12" title="">
      <form class="flex flex-col gap-4" @submit.prevent="signin">
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
