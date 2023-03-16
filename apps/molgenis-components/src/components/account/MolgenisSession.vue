<template>
  <Spinner v-if="loading" />
  <div v-else>
    <div>
      <span v-if="session.email && session.email != 'anonymous'">
        <ButtonAlt @click="showChangePasswordForm = true" class="text-light">
          Hi {{ session.email }}</ButtonAlt
        >&nbsp;
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
          Sign in</ButtonOutline
        >
        <ButtonOutline v-else @click="showSigninForm = true" :light="true">
          Sign in</ButtonOutline
        >
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

<script lang="ts">
import { storeToRefs } from "pinia";
import { defineComponent } from "vue";
import { request } from "../../client/client.js";
import { useSessionStore } from "../../stores/sessionStore";
import ButtonAlt from "../forms/ButtonAlt.vue";
import ButtonOutline from "../forms/ButtonOutline.vue";
import Spinner from "../layout/Spinner.vue";
import { ISession } from "./Interfaces";
import LocaleSwitch from "./LocaleSwitch.vue";
import MolgenisAccount from "./MolgenisAccount.vue";
import MolgenisSignin from "./MolgenisSignin.vue";
import SignupForm from "./MolgenisSignup.vue";

const defaultSession: ISession = { locale: "en", settings: {} };
const store = useSessionStore();
const { session } = storeToRefs(store);

/** Element that is supposed to be put in menu holding all controls for user account */
export default defineComponent({
  components: {
    ButtonOutline,
    MolgenisSignin,
    SignupForm,
    MolgenisAccount,
    Spinner,
    ButtonAlt,
    LocaleSwitch,
  },
  props: {
    graphql: {
      default: "graphql",
      type: String,
    },
  },
  data: function () {
    return {
      showSigninForm: false,
      showSignupForm: false,
      showChangePasswordForm: false,
      error: null as string | null,
      loading: false,
      session: defaultSession,
      version: null,
    };
  },
  watch: {
    email() {
      this.showSigninForm = false;
      this.showSignupForm = false;
    },
  },
  created() {
    this.reload();
  },
  computed: {
    isOidcEnabled() {
      return this.session?.settings?.isOidcEnabled === "true";
    },
    locales() {
      if (this.session?.settings?.locales) {
        if (Array.isArray(this.session.settings.locales)) {
          return this.session.settings.locales;
        } else {
          this.error =
            'locales should be array similar to ["en"] but instead was ' +
            JSON.stringify(this.session.settings.locales);
        }
      }
      //default
      return ["en"];
    },
  },
  methods: {
    changed() {
      // this.reload();
      this.showSigninForm = false;
      this.$emit("update:modelValue", this.session);
    },
    closeSigninForm() {
      this.showSigninForm = false;
      this.error = null;
    },
    closeSignupForm() {
      this.showSignupForm = false;
      this.error = null;
    },
    async signout() {
      this.loading = true;
      this.showSigninForm = false;
      const data = await request("graphql", `mutation{signout{status}}`).catch(
        (error: string) => (this.error = "internal server error" + error)
      );
      if (data.signout.status === "SUCCESS") {
        // this.session = {};
      } else {
        this.error = "sign out failed";
      }
      this.loading = false;
      this.$emit("update:modelValue", this.session);
      // this.reload();
    },
  },
  emits: ["update:modelValue", "error"],
});
</script>

<docs>
<template>
  <div>
    <MolgenisSession class="bg-primary" v-model="session" graphql="/pet store/graphql"/>
    <pre>session = {{ session }}</pre>
  </div>
</template>
<script>
  export default {
    data() {
      return {
        session: null
      }
    }
  }
</script>
</docs>
