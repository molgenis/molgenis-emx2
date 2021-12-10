<template>
  <div class="card">
    <div class="row no-gutters">
      <div class="col-8 small">
        <strong>
          <span v-if="contact.title && contact.title.name">{{
            contact.title.name
          }}</span>
          <span v-if="contact.initials">{{ contact.initials }}</span>
          <span v-if="contact.firstName"> ({{ contact.firstName }})</span>
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

        <!-- {{ contact }} -->
      </div>
      <div class="col-4">
        <img
          :src="imgSrc"
          :alt="contact.surname + '-image'"
          class="img-thumbnail"
        />
      </div>
    </div>
  </div>
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
export default {
  name: "ContactDetails",
  props: {
    contact: {
      type: Object,
      required: false,
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
        ? this.contact.photo.url
        : require("@/assets/user-placeholder.png");
    },
  },
};
</script>
