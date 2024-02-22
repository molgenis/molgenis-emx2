<script setup lang="ts">
const modalType = ref("success");
let showModal = ref(false);
let label = ref("Show modal");

function handelClick() {
  label.value = "showing";
  showModal.value = true;
}

function handelClose() {
  label.value = "Show modal";
  showModal.value = false;
}
</script>
<template>
  <LayoutsDetailPage>
    <template #header>
      <PageHeader title="Playground">
        <template #prefix>
          <BreadCrumbs :crumbs="[]" />
        </template>
      </PageHeader>
    </template>
    <template #main>
      <Button
        :label="label"
        type="tertiary"
        size="small"
        @click="handelClick"
      />

      <div>
        type:

        <input
          class="mr-2 mt-4"
          type="radio"
          id="light"
          value="light"
          v-model="modalType"
        />
        <label for="light">light</label>

        <input
          class="mr-2 ml-4"
          type="radio"
          id="success"
          value="success"
          v-model="modalType"
        />
        <label for="success">success</label>

        <input
          class="mr-2 ml-4"
          type="radio"
          id="error"
          value="error"
          v-model="modalType"
        />
        <label for="error">error</label>
      </div>
      <NotificationModal
        v-if="showModal"
        :type="modalType"
        subTitle="sub title"
        title="Title"
        @close="handelClose"
      >
        <p>
          Good resolutions are useless attempts to interfere with scientific
          laws. Their origin is pure vanity. Their result is absolutely nil.
          They give us, now and then, some of those luxurious sterile emotions
          that have a certain charm for the weak. That is all that can be said
          for them. They are simply cheques that men draw on a bank where they
          have no account.
        </p>
      </NotificationModal>
    </template>
  </LayoutsDetailPage>
</template>
