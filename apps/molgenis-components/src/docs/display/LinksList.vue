<template>
  <demo-item id="links-list" label="Links list">
    <label class="font-italic">basic list of links</label>
    <links-list
      :items="[
        { label: 'Molgenis', href: 'https://molgenis.org' },
        { label: 'Google', href: 'https://google.com' },
      ]"
    ></links-list>
  </demo-item>
</template>

