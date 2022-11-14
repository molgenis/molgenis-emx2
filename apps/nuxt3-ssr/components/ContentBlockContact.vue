<script setup>
import ContentBlock from "./ContentBlock.vue";
import ContactCardList from "./ContactCardList.vue";
import ContactCard from "./ContactCard.vue";

defineProps({
  title: {
    type: String,
  },
  description: {
    type: String,
  },
});
</script>

<template>
  <ContentBlock :title="title">
    <ContactCardList>
      <ContactCard
        title="Principal investigator"
        name="SJ (Soesma) Jankipersadsign"
        email="s.a.jankipersadsing@umcg.nl"
        href="mailto:s.a.jankipersadsing@umcg.nl"
      />
      <ContactCard
        name="SJ (Soesma) Jankipersadsign"
        email="s.a.jankipersadsing@umcg.nl"
        href="mailto:s.a.jankipersadsing@umcg.nl"
      />
      <ContactCard
        title="Principal investigator"
        name="SJ (Soesma) Jankipersadsign"
        email="s.a.jankipersadsing@umcg.nl"
        href="mailto:s.a.jankipersadsing@umcg.nl"
      />
    </ContactCardList>
  </ContentBlock>
</template>
