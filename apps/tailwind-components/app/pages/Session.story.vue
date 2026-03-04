<script setup lang="ts">
import { ref } from "vue";
import { useSession } from "../composables/useSession";
const { isAdmin, session, reload, hasSessionTimeout } = await useSession();
import Field from "../components/Field.vue";

const timeoutCheckResponse = ref("unknown");
const username = ref("admin");
const password = ref("");
const error = ref("");

async function signin() {
  if (!username.value || !password.value) {
    error.value = "Email and password should be filled in";
  } else {
    error.value = "";

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
    });

    if (signinResp.data.signin.status === "SUCCESS") {
      await reload();
      timeoutCheckResponse.value = "unknown";
    } else {
      console.log(signinResp.data.signin.message);
      error.value = signinResp.data.signin.message;
    }
  }
}

async function signout() {
  const { data, error } = await $fetch("/api/graphql", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: { query: `mutation { signout { status } }` },
  });

  if (error || data.signout.status !== "SUCCESS") {
    console.error("Error signing out:", error);
    return;
  }

  reload();
  timeoutCheckResponse.value = "unknown";
}
</script>
<template>
  <DefinitionList class="mt-4 pl-4 pt-2 border border-solid border-border-base">
    <DefinitionListTerm
      class="text-definition-list-term text-body-base font-light capitalize"
      >Is admin
    </DefinitionListTerm>
    <DefinitionListDefinition class="text-title-contrast">
      {{ isAdmin }}
    </DefinitionListDefinition>
    <DefinitionListTerm
      class="text-definition-list-term text-body-base font-light capitalize"
      >Session
    </DefinitionListTerm>
    <DefinitionListDefinition class="text-title-contrast">
      <DefinitionList class="pl-4 border border-solid border-border-base">
        <DefinitionListTerm
          class="text-definition-list-term text-body-base font-light capitalize"
        >
          admin
        </DefinitionListTerm>
        <DefinitionListDefinition class="text-title-contrast">
          {{ session?.admin }}
        </DefinitionListDefinition>
        <DefinitionListTerm
          class="text-definition-list-term text-body-base font-light capitalize"
        >
          email
        </DefinitionListTerm>
        <DefinitionListDefinition class="text-title-contrast">
          {{ session?.email }}
        </DefinitionListDefinition>
        <DefinitionListTerm
          class="text-definition-list-term text-body-base font-light capitalize"
        >
          roles
        </DefinitionListTerm>
        <DefinitionListDefinition class="text-title-contrast">
          {{ session?.roles || "none" }}
        </DefinitionListDefinition>
        <DefinitionListTerm
          class="text-definition-list-term text-body-base font-light capitalize"
        >
          token
        </DefinitionListTerm>
        <DefinitionListDefinition class="text-title-contrast">
          {{ session?.token?.substring(0, 13).padEnd(16, ".") || "none" }}
        </DefinitionListDefinition>
      </DefinitionList>
    </DefinitionListDefinition>
  </DefinitionList>

  <div
    class="mt-4 border border-solid border-border-base p-4 flex flex-col gap-8"
  >
    <Button type="outline" size="medium" @click="reload">
      Reload session
    </Button>

    <Button
      type="outline"
      size="medium"
      @click="
        async () => {
          timeoutCheckResponse = String(await hasSessionTimeout());
        }
      "
    >
      test for session timeout
    </Button>

    <span>Has session timed out ?: {{ timeoutCheckResponse }}</span>

    <hr />

    <Field label="user name" v-model="username" id="user-name" type="STRING" />
    <Field label="password" v-model="password" id="user-pw" type="STRING" />
    <Button type="outline" size="medium" @click="signin"> Sign in </Button>
    <Button type="outline" size="medium" @click="signout"> Sign out </Button>
    <span class="text-invalid">{{ error }}</span>
  </div>
</template>
