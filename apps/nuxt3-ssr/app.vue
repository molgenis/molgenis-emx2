<template>
  <div
    class="min-h-screen bg-base-gradient relative after:bg-app-wrapper after:w-full after:h-[166px] after:top-0 after:absolute after:opacity-20 after:z-20 xl:after:hidden">
    <div class="absolute top-0 left-0 z-10 w-screen h-screen overflow-hidden opacity-background-gradient">
      <BackgroundGradient class="z-10" />
    </div>
    <div class="z-30 relative">
      <slot name="header">
        <AppHeader />
      </slot>
      <main>
        <slot>
          <NuxtPage />
        </slot>
      </main>
      <slot name="footer">
        <div class="mt-5 mb-0 text-center text-body-lg"> This database was created using the
          <a class="text-body-base text-blue-500 hover:underline hover:bg-blue-50"
            href="http://molgenis.org">MOLGENIS</a>&nbsp;
          <a class="text-body-base text-blue-500 hover:underline hover:bg-blue-50"
            href="http://github.com/molgenis/molgenis-emx2"> molgenis-emx2 </a>
          open source software (license: <a class="text-body-base text-blue-500 hover:underline hover:bg-blue-50"
            href="https://github.com/molgenis/molgenis-emx2/blob/master/LICENSE">LGPLv3</a>).
        </div>
        <div class="mb-0 text-center lg:pb-5 text-body-lg"> Please cite <a
            class="text-body-base text-blue-500 hover:underline hover:bg-blue-50"
            href="https://www.ncbi.nlm.nih.gov/pubmed/30165396"> Van der
            Velde et al (2018)</a> or <a href="https://www.ncbi.nlm.nih.gov/pubmed/21210979"> Swertz et al
            (2010)</a> on use. </div>
      </slot>
    </div>
  </div>
</template>

<script>
import "@/assets/css/main.css";
import BackgroundGradient from "./components/BackgroundGradient.vue";
</script>
