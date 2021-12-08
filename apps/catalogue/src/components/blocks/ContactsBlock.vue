<template>
  <div class="">
    <h5>{{ heading }}</h5>
    <div class="d-flex flex-row">
      <contact-details class="mr-1"
        v-for="(item, index) in items"
        :key="index"
      ></contact-details>
    </div>
  </div>
</template>

<script>
import ContactDetails from "../ContactDetails.vue";
export default {
  name: "ContactsBlock",
  components: { ContactDetails },
  props: {
    heading: {
      type: String,
      required: true,
    },
    /**
     * List contact pids
     * Contact pid :
     * fistName: String (required)
     * surname: String (required)
     */
    items: {
      type: Array,
      required: false,
      default: () => [],
    },
  },
};
</script>

<style></style>
