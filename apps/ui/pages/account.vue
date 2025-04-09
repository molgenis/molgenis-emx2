<script setup lang="ts">
import { navigateTo, useRoute, useRouter } from "#app/composables/router";
import { ref } from "vue";
import { useSession } from "../composables/session";

const route = useRoute();
const router = useRouter();
const { session, reload: reloadSession } = await useSession();

const errorMsg = ref("");
async function signout() {
  const { data, error } = await $fetch("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: { query: `mutation { signout { status } }` },
  }).catch((error) => {
    errorMsg.value = "internal server graphqlError" + error;
  });

  if (error) {
    errorMsg.value = error;
  }

  if (data.signout.status === "SUCCESS") {
    console.log(data.signout.message);

    reloadSession();
    route.redirectedFrom ? router.go(-1) : navigateTo({ path: "/" });
  } else {
    console.log(data.signout.message);
  }
}
</script>
<template>
  <Container class="flex flex-col items-center">
    <ContentBlock class="w-6/12 mt-3" :title="session?.email">
      <div>
        <Button
          v-if="session?.email !== 'anonymous'"
          type="primary"
          size="medium"
          @click="signout"
        >
          Sign out
        </Button>
        <Button
          v-else
          type="primary"
          size="medium"
          @click="navigateTo('/login')"
        >
          Sign in
        </Button>
      </div>

      <div class="mt-3">{{ errorMsg }}</div>
    </ContentBlock>
  </Container>
</template>
