<template>
  <div class="text-center">
    <span>
      This database was created using the
      <a href="http://molgenis.org">MOLGENIS</a>&nbsp;
      <a href="http://github.com/mswertz/molgenis-emx2"> molgenis-emx2 </a>
      open source software (license:
      <a href="https://github.com/molgenis/molgenis-emx2/blob/master/LICENSE"
        >LGPLv3</a
      >).
    </span>
    <span>
      Please cite
      <a href="https://www.ncbi.nlm.nih.gov/pubmed/30165396">
        Van der Velde et al (2018)</a
      >
      or
      <a href="https://www.ncbi.nlm.nih.gov/pubmed/21210979">
        Swertz et al (2010)</a
      >
      on use.
    </span>
    <br />
    <slot />
  </div>
</template>

<script>
export default {};
</script>

<docs>
```
<MolgenisFooter/>
```
</docs>
