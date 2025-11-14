<template>
  <div class="flex gap-16 flex-col">
    <div>
      <p class="pb-3 text-title">
        Simple pageheader (default alignment):
      </p>
      <div>
        <PageHeader title="My Title" description='My description' />
      </div>
    </div>
  <div>
    <p class="pb-3 text-title">
      Center-aligned Pageheader with title prefix/suffix, icon and long description:
    </p>
    <div>
    <PageHeader title="My Title" icon="handshake" description='This is some dummy text to create a string of over 250 characters long to show that it is initially truncated. You can view the full text by pressing "..." at the end of this description to view the text that is initially hidden but then suddenly appears and allows the user to read the full description.'>
      <template #title-prefix>
<!--        <ButtonPageHeader label="ButtonPageHeader in title-prefix slot" icon="arrow-left" />-->
        <span>title-prefix text</span>
      </template>
      <template #title-suffix>
        <span>a much longer text in title-suffix slot (title stays centered)</span>
      </template>
    </PageHeader>
    </div>
  </div>
    <div>
      <p class="pb-3 text-title">
        Left-aligned Pageheader with all optional slots filled:
      </p>
    <PageHeader align="left" icon="handshake" title="A very long title to that needs multiple lines to see how line wrapping/alignment is handled">
      <template #description>
        <span>Description through slot (instead of prop)</span><CustomTooltip
            label="tooltip"
            content="that has a tooltip component inside of it"
        />
      </template>
      <template #title-prefix>
        <ButtonPageHeader label="ButtonPageHeader in title-prefix slot" icon="arrow-left" />
      </template>
      <template #title-suffix>
        <ButtonPageHeader label="ButtonPageHeader in title-suffix slot" icon="star" />
        <ButtonPageHeader label="another ButtonPageHeader in title-suffix slot" icon="trash" />
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
<script setup lang="ts">
import ButtonPageHeader from "../components/button/PageHeader.vue";
import CustomTooltip from "../components/CustomTooltip.vue";
</script>