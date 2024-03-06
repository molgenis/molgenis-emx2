<script setup>
import { computed, ref } from "vue";
import { useCookies } from "@vueuse/integrations/useCookies";
import LayoutModal from "./LayoutModal.vue";
import ButtonAction from "../forms/ButtonAction.vue";
import ButtonAlt from "../forms/ButtonAlt.vue";
import GTag from "./GTag.vue";
const cookies = useCookies(["mg_allow_analytics"]);
const props = defineProps({
  title: {
    type: String,
    default: "Would you like a cookie?",
  },
  acceptLabel: {
    type: String,
    default: "Accept",
  },
  rejectLabel: {
    type: String,
    default: "No thanks",
  },
  analyticsId: {
    type: String,
  },
  htmlContentString: {
    type: String | null,
    default: null,
  },
});

const emit = defineEmits(["acceptCookie"]);

let isDeclined = ref(false);

const show = computed(() => {
  return cookies.get("mg_allow_analytics") === undefined;
});

const cookieOptions = {
  path: "/",
  maxAge: 1704085200, // one year,
  httpOnly: false,
};

function handleAccept() {
  cookies.set("mg_allow_analytics", true, cookieOptions);
  emit("acceptCookie", true);
  window.location.reload();
}

function handleDecline() {
  cookies.set("mg_allow_analytics", false, cookieOptions);
  emit("acceptCookie", false);
}

function handleClose() {
  isDeclined.value = true;
  emit("acceptCookie", undefined);
}

const isAnalyticsEnabled = computed(() => {
  return cookies.get("mg_allow_analytics") === true;
});
</script>

<template>
  <LayoutModal :title="title" @close="handleClose" :show="show && !isDeclined">
    <template v-slot:body>
      <div v-if="htmlContentString" v-html="htmlContentString"></div>
      <div v-else>
        <p>
          We use cookies and similar technologies to enhance your browsing
          experience, analyze website traffic, and personalize content. By
          clicking "Accept," you consent to the use of cookies.
        </p>
        <p>
          We value your privacy and are committed to protecting your personal
          information. Our use of cookies is solely for improving your
          experience on our website and ensuring its functionality. We do not
          sell or share your data with third parties.
        </p>
      </div>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="handleDecline">{{ rejectLabel }}</ButtonAlt>
      <ButtonAction @click="handleAccept">{{ acceptLabel }}</ButtonAction>
    </template>
  </LayoutModal>
  <GTag v-if="isAnalyticsEnabled" :tagId="analyticsId" />
</template>

<docs>
<template>
  <demo-item>
    <CookieWall @acceptCookie="handleAcceptCookie" analyticsId="fake"></CookieWall>
    <div class="mb-3">cookie 'mg_allow_analytics' is set to: {{cookieValue}}</div>
    <p>You can replace the default message by placing a custom message in de default slot</p>
    <p>Override the default button labels using the 'acceptLabel' and 'rejectLabel' props</p>
     <ButtonAction @click="resetDemo">reset demo</ButtonAction>
  </demo-item>
</template>
<script setup>
import { computed, ref } from "vue";
import { useCookies } from "@vueuse/integrations/useCookies";
const cookies = useCookies();

let cookieValue  = ref(cookies.get('mg_allow_analytics'));

function handleAcceptCookie (value) {
  console.log('handleAcceptCookie', value);
  cookieValue.value  = value;
}

function resetDemo () {
  cookies.remove('mg_allow_analytics');
  window.location.reload();
}
   
</script>
</docs>
