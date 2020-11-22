<template>
  <p class="text-center">
    This database was created using the open source
    <a href="http://molgenis.org">MOLGENIS</a>
    <a href="http://github.com/mswertz/molgenis-emx2"
      >molgenis-emx2 open source</a
    >
    software.
    <slot />
    . <br />Please cite
    <a href="https://www.ncbi.nlm.nih.gov/pubmed/30165396"
      >Van der Velde et al (2018)</a
    >
    or
    <a href="https://www.ncbi.nlm.nih.gov/pubmed/21210979"
      >Swertz et al (2010)</a
    >
    on use.
  </p>
</template>

<script>
export default {};
</script>

<docs>
```
<Footer/>
```
</docs>
