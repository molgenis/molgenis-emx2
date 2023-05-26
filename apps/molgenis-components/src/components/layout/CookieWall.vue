<script setup>
import { computed, ref } from "vue";
import { useCookies } from "@vueuse/integrations/useCookies";
import LayoutModal from "./LayoutModal.vue";
import GTag from "./GTag.vue";
const cookies = useCookies(["mg_allow_analytics"]);
const props = defineProps({
  title: {
    type: String,
    default: "Would you like a cookie ?",
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
});

const emit = defineEmits(["acceptCookie"]);

let isDeclined = ref(false);

const show = computed(() => {
  return cookies.get("mg_allow_analytics") === undefined;
});

function handleAccept() {
  cookies.set("mg_allow_analytics", true);
  emit("acceptCookie", true);
  window.location.reload();
}

function handleDecline() {
  cookies.set("mg_allow_analytics", false);
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
      <slot>
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
      </slot>
    </template>
    <template v-slot:footer>
      <ButtonAlt @click="handleDecline">No thanks</ButtonAlt>
      <ButtonAction @click="handleAccept">{{ acceptLabel }}</ButtonAction>
    </template>
  </LayoutModal>
  <GTag v-if="isAnalyticsEnabled" :tagId="analyticsId" />
</template>

<docs>
<template>
  <demo-item>
    <CookieWall @acceptCookie=handleAcceptCookie></CookieWall>
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
