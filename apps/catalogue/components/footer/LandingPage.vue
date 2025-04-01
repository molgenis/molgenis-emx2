<template>
  <footer class="bg-footer text-white">
    <div
      class="grid grid-cols-1 gap-4 lg:gap-2 lg:grid-cols-3 items-start justify-center lg:max-w-[1024px] m-auto px-6 pt-6"
    >
      <div>
        <h3 class="uppercase mb-2 text-heading-lg font-display tracking-widest">
          Created with MOLGENIS
        </h3>
        <ul>
          <li class="flex items-ceter">
            <a
              class="flex items-center hover:underline"
              href="http://molgenis.org"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>About Molgenis</span>
            </a>
          </li>
          <li>
            <a
              class="flex items-center hover:underline"
              href="http://molgenis.org"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>MOLGENIS.org</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li class="flex items-center">
            <BaseIcon name="CaretRight" :width="16" />
            <span>Need help?</span>
            <a href="mailto:support@molgenis.org" class="ml-1 underline"
              >Contact us</a
            >
          </li>
        </ul>
      </div>
      <div>
        <h3 class="uppercase mb-2 text-heading-lg font-display tracking-widest">
          Running a custom MOLGENIS version
        </h3>
        <ul class="list-style-none flex flex-col mb-5">
          <li>
            <a
              class="flex items-center hover:underline"
              href="https://github.com/molgenis/molgenis-emx2"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>MOLGENIS on GitHub</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li>
            <a
              class="flex items-center hover:underline"
              href="https://molgenis.org/LICENSE-LGPLv3.html"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Open Source LGPLv3 License</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li>
            <a
              class="flex items-center hover:underline"
              href="https://molgenis.org/MOLGENIS_Privacy_Policy.pdf"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Privacy Policy</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
        </ul>
      </div>
      <div>
        <h3 class="uppercase mb-2 text-heading-lg font-display tracking-widest">
          Please cite on use
        </h3>
        <ul class="list-style-none flex flex-col mb-5">
          <li>
            <a
              class="hover:underline flex items-center"
              href="https://www.ncbi.nlm.nih.gov/pubmed/36463884"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Swertz & Gini (2022)</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li>
            <a
              class="hover:underline flex items-center"
              href="https://www.ncbi.nlm.nih.gov/pubmed/30165396"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Van der Velde et al (2018)</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
          <li>
            <a
              class="hover:underline flex items-center"
              href="https://www.ncbi.nlm.nih.gov/pubmed/21210979"
            >
              <BaseIcon name="CaretRight" :width="16" />
              <span>Swertz et al (2010)</span>
              <BaseIcon name="ExternalLink" :width="16" />
            </a>
          </li>
        </ul>
      </div>
    </div>
    <div class="pb-2">
      <ClientOnly>
        <FooterVersion />
      </ClientOnly>
    </div>
  </footer>
</template>
