<template>
  <demo-item id="contact-display" label="Contact Display">
    <div class="row">
      <div class="col-3">
        <contact-display
          :contact="contact"
          :contributionType="contributions"
          :contributionDescription="contributionDescription"
        ></contact-display>
      </div>
    </div>
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




