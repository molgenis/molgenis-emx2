<script setup lang="ts">
import ButtonPageheaderBack from "../components/button/PageHeaderBack.vue";
import Hyperlink from "~/components/text/Hyperlink.vue";
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
        Center-aligned Pageheader using description slot and back icon:
      </p>
      <PageHeader icon="handshake" title="My Title">
        <template #description>
          <span>My description using a slot with </span
          ><Hyperlink to="https://molgenis.org" label="a Hyperlink" /><span>
            in between 2 spans.</span
          >
        </template>
        <template #title-prefix>
          <ButtonPageheaderBack />
        </template>
      </PageHeader>
    </div>
    <div>
      <p class="pb-3 text-title">
        Left-aligned Pageheader using description slot and back icon:
      </p>
      <PageHeader align="left" icon="handshake" title="My Title">
        <template #description>
          <span>My description using a slot with </span
          ><Hyperlink to="https://molgenis.org" label="a Hyperlink" /><span>
            in between 2 spans.</span
          >
        </template>
        <template #title-prefix>
          <ButtonPageheaderBack label="custom button text" />
          <span>icon and text in title-prefix</span>
        </template>
      </PageHeader>
    </div>
  </div>
  <div>
    <p class="pb-3 text-title">
      Center-aligned Pageheader with text-based (title) prefix/suffix & long
      description that is truncated:
    </p>
    <div>
      <PageHeader
        title="My Title"
        description='This is some dummy text to create a string of over 250 characters long to show that it is initially truncated. You can view the full text by pressing "..." at the end of this description to view the text that is initially hidden but then suddenly appears and allows the user to read the full description.'
      >
        <template #title-prefix>
          <span>title-prefix text</span>
        </template>
        <template #title-suffix>
          <span
            >a much longer text in title-suffix slot (title stays
            centered)</span
          >
        </template>
        <template #prefix>
          <span>prefix</span>
        </template>
        <template #suffix>
          <span>suffix</span>
        </template>
      </PageHeader>
    </div>
  </div>
</template>
