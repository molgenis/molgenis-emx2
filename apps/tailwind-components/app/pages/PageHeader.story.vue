<script setup lang="ts">
import TextHyperlink from "../components/text/Hyperlink.vue";
</script>
<template>
  <div class="flex gap-16 flex-col">
    <div>
      <p class="pb-3 text-title">Simple pageheader (default alignment):</p>
      <div>
        <PageHeader title="My Title" description="My description" />
      </div>
    </div>
    <div>
      <p class="pb-3 text-title">Simple pageheader (left aligned):</p>
      <div>
        <PageHeader
          align="left"
          title="My Title"
          description="My description"
        />
      </div>
    </div>
    <div>
      <p class="pb-3 text-title">
        Center-aligned Pageheader with back button using description slot:
      </p>
      <PageHeader icon="handshake" title="My Title" backPath="../">
        <template #description>
          <span>My description using a slot with </span
          ><TextHyperlink to="https://molgenis.org" label="a Hyperlink" type="external" /><span>
            in between 2 spans.</span
          >
        </template>
      </PageHeader>
    </div>
    <div>
      <p class="pb-3 text-title">
        Left-aligned Pageheader with back button using description slot:
      </p>
      <PageHeader align="left" icon="handshake" title="My Title" backPath="/">
        <template #description>
          <span>My description using a slot with </span
          ><TextHyperlink to="https://molgenis.org" label="a Hyperlink" type="external" /><span>
            in between 2 spans.</span
          >
        </template>
      </PageHeader>
    </div>
  </div>
</template>
