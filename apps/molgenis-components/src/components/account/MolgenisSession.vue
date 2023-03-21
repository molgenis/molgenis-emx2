<template>
  <Spinner v-if="loading" />
  <div v-else>
    <div>
      <span v-if="session?.email !== 'anonymous'">
        <ButtonAlt @click="showChangePasswordForm = true" class="text-light">
          Hi {{ session.email }}
        </ButtonAlt>
        &nbsp;
        <MolgenisAccount
          v-if="showChangePasswordForm"
          :error="error"
          @cancel="showChangePasswordForm = false"
        />
        <ButtonOutline @click="signout" :light="true">Sign out</ButtonOutline>
      </span>
      <span v-else>
        <ButtonAlt
          v-show="!isOidcEnabled"
          @click="showSignupForm = true"
          :light="true"
        >
          Sign up
        </ButtonAlt>
        <SignupForm
          v-if="showSignupForm"
          :error="error"
          @close="closeSignupForm"
        />
        <ButtonOutline v-if="isOidcEnabled" href="/_login" :light="true">
          Sign in
        </ButtonOutline>
        <ButtonOutline v-else @click="showSigninForm = true" :light="true">
          Sign in
        </ButtonOutline>
        <MolgenisSignin
          v-if="showSigninForm"
          @signin="changed"
          @cancel="closeSigninForm"
        />
      </span>
      <LocaleSwitch
        v-if="locales.length > 1"
        class="ml-2"
        v-model="session.locale"
        :locales="locales"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from "pinia";
import { computed, ref } from "vue";
import { useSessionStore } from "../../stores/sessionStore";
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonOutline from "../forms/ButtonOutline.vue";
import Spinner from "../layout/Spinner.vue";
import LocaleSwitch from "./LocaleSwitch.vue";
import MolgenisAccount from "./MolgenisAccount.vue";
import MolgenisSignin from "./MolgenisSignin.vue";
import SignupForm from "./MolgenisSignup.vue";

let showSigninForm = ref(false);
let showSignupForm = ref(false);
let showChangePasswordForm = ref(false);
let loading = ref(false);

const sessionStore = useSessionStore();
const { graphqlError: error, session } = storeToRefs(sessionStore);

const isOidcEnabled = computed(
  () => session.value?.settings?.isOidcEnabled === "true"
);

const locales = computed(() => session.value?.settings?.locales || ["en"]);

function changed() {
  showSigninForm.value = false;
  sessionStore.signin();
}

function closeSigninForm() {
  showSigninForm.value = false;
  error.value = "";
}

function closeSignupForm() {
  showSignupForm.value = false;
  error.value = "";
}

async function signout() {
  loading.value = true;
  await sessionStore.signout();
  showSigninForm.value = false;
  loading.value = false;
}
</script>

<docs>
<template>
  <div>
    <MolgenisSession class="bg-primary" />
    <pre>session = {{ JSON.stringify(session, null, 2) }}</pre>
  </div>
</template>

<script lang="ts">
import { storeToRefs } from "pinia";

export default {
    data() {
      return {
        session: storeToRefs(this.$sessionStore)
      }
    }
  }
</script>
</docs>
