<script setup>
import IconButton from "./IconButton.vue";
</script>

<template>
  <article class="p-12.5">
    <header class="flex items-center">
      <h2
        class="text-heading-base font-extrabold text-blue-500 min-w-[160px] inline-block mr-4"
      >
        LIFELINES NEXT
      </h2>

      <span class="text-heading-base grow">Lifelines NEXT</span>

      <div class="flex">
        <IconButton icon="star" class="text-blue-500" />
        <IconButton icon="arrow-right" class="text-blue-500" />
      </div>
    </header>

    <p class="text-body-base my-5">
      The European Human Exposome Network (EHEN) is the world’s largest network
      of projects studying the impact of environmental exposures across a
      lifetime - the exposome - on human health. Collectively, the EHEN projects
      are working in 24 countries across Europe and the USA. There are over 100
      data sets being used including cohorts, cross-sectional studies, biobanks,
      randomised controlled trials (RCTs), case control studies and health or
      occupational registries.
    </p>

    <dl class="flex flex-col md:flex-row gap-5 md:gap-14 text-body-base">
      <div>
        <dt class="flex-auto block text-gray-600">Keywords</dt>
        <dd>Primary care data, general practice, morbidity</dd>
      </div>
      <div>
        <dt class="flex-auto block text-gray-600">Type</dt>
        <dd>Clinical cohort, Population cohort</dd>
      </div>
      <div>
        <dt class="flex-auto block text-gray-600">Design</dt>
        <dd>Longitudinal</dd>
      </div>
    </dl>
  </article>
</template>

<!-- 
      <dl class="flex flex-col md:flex-row gap-5 md:gap-14 text-body-base">
      <dt>
        <span class="flex-auto block text-gray-400">Keywords</span>
        Primary care data, general practice, morbidity
      </dt>
      <dt>
        <span class="flex-auto block text-gray-400">Type</span>
        Clinical cohort, Population cohort
      </dt>
      <dt>
        <span class="flex-auto block text-gray-400">Design</span>
        Longitudinal
      </dt>
    </dl>
-->
