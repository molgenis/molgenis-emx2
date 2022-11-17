<script setup>
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
  <ContentBlock :title="title" :description="description">
    <ReferenceCardList>
      <ReferenceCard
        imageUrl="https://via.placeholder.com/137x150"
        title="Placeholder network"
        description="The European Human Exposome Network (EHEN) is the world’s largest
          network of projects studying the impact of environmental exposures
          across a lifetime - the exposome - on human health. Collectively, the
          EHEN projects are working in 24 countries across Europe and the USA.
          There are over 100 data sets being used including cohorts,
          cross-sectional studies, biobanks, randomised controlled trials
          (RCTs), case control studies and health or occupational registries."
        url="#"
        :links="[
          { title: 'Network', url: '#' },
          { title: 'Cohorts', url: '#' },
          { title: 'Variables', url: '#' },
        ]"
      />
      <ReferenceCard
        imageUrl="https://via.placeholder.com/137x150"
        title="Placeholder network"
        description="The European Human Exposome Network (EHEN) is the world’s largest
          network of projects studying the impact of environmental exposures
          across a lifetime - the exposome - on human health. Collectively, the
          EHEN projects are working in 24 countries across Europe and the USA.
          There are over 100 data sets being used including cohorts,
          cross-sectional studies, biobanks, randomised controlled trials
          (RCTs), case control studies and health or occupational registries."
        url="#"
        :links="[
          { title: 'Network', url: '#' },
          { title: 'Cohorts', url: '#' },
          { title: 'Variables', url: '#' },
        ]"
      />
    </ReferenceCardList>
  </ContentBlock>
</template>
