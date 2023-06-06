<template>
  <person-details>
    <template #details>
      <strong>
        <span v-if="contact.title && contact.title.name">{{
          contact.title.name
        }}</span>
        <span v-if="contact.initials"> {{ contact.initials }}</span>
        <span v-if="contact.firstName && contact.initials">
          ({{ contact.firstName }})
        </span>
        <span v-else-if="contact.firstName"> {{ contact.firstName }}</span>
        <span v-if="contact.surname"> {{ contact.surname }}</span>
      </strong>
      <div v-if="contributions.length">{{ contributions }}</div>
      <div v-if="contact.email">{{ contact.email }}</div>
      <div v-if="contributionDescription" class="mt-3">
        <p>{{ contributionDescription }}</p>
      </div>

      <div v-if="contact.department" class="mt-3">
        <p>{{ contact.department }}</p>
      </div>
    </template>
    <template v-if="imgSrc" #image>
      <img
        :src="imgSrc"
        :alt="contact.surname + '-image'"
        class="img-thumbnail"
      />
    </template>
  </person-details>
</template>

<style scoped>
.card {
  border: 0;
}

.img-thumbnail {
  border: 0;
  padding: 0;
  border-radius: 0;
}
</style>

<script>
import PersonDetails from "./PersonDetails.vue";

export default {
  name: "ContactDisplay",
  components: { PersonDetails },
  props: {
    contact: {
      type: Object,
      required: true,
      default: () => {},
    },
    contributionType: {
      type: Array,
      required: false,
      default: () => [],
    },
    contributionDescription: {
      type: String,
      required: false,
    },
  },
  computed: {
    contributions() {
      return [...this.contributionType]
        .sort((a, b) => a.order - b.order)
        .map((ct) => ct.name)
        .join(",");
    },
    imgSrc() {
      return this.contact.photo && this.contact.photo.url
        ? this.contact.photo.url // or else show place-holder image as encoded png
        : null;
    },
  },
};
</script>

<docs>
<template>
  <demo-item>
    <contact-display
        :contact="contact"
        :contributionType="contributions"
        :contributionDescription="contributionDescription"
    ></contact-display>
  </demo-item>
</template>
<script>
  export default {
    data() {
      return {
        contact: {
          firstName: "John",
          surname: "Doe",
          initials: "J.J.",
          title: {
            name: "Mr.",
          },
          email: "j.j.doe@world-online.com",
          department: "craft services",
        },
        contributions: [
          {
            name: "lead",
            oder: 1,
          },
        ],
        contributionDescription: "getting coffee",
      };
    },
  };
</script>
</docs>
