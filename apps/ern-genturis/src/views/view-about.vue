<template>
  <Page id="page-about">
    <PageHeader
      class="genturis-header"
      title="ERN Genturis Registry"
      subtitle="Registry for Genetic Tumour Risk Syndromes"
      imageSrc="genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection id="genturis-section-about" aria-labelledby="genturis-section-about-title" :verticalPadding="2" width="large">
      <h2 id="genturis-section-about-title">About the GENTURIS registry</h2>
      <p>The <strong>GENTURIS</strong> registry is a web-based platform that facilitates and accelerates European research to improve diagnostics, treatment and the provision of high-quality healthcare for patients with one of the genetic tumour risk syndromes (genturis).</p>
      <p>The GENTURIS registry is affiliated to the European Reference Network for all patients with one of the genetic tumour risk syndromes (ERN GENTURIS). ERN GENTURIS is a network of many expert centres in over 20 European countries.</p>
      <p>The GENTURIS registry consists of a collection of common data elements for all genturis patients, combined with disease-specific data elements for all thematic disease groups covered by ERN GENTURIS. The FAIR Data Point (FDP), a machine-readable description of the metadata, can be found at: <a href="https://genturis-registry.eu/api/fdp">https://genturis-registry.eu/api/fdp</a>.</p>
      <h3>Thematic disease groups</h3>
      <p>For additional information on ERN GENTURIS thematic disease groups, please see <a href="https://www.genturis.eu/l=eng/Thematic-disease-groups.html">https://www.genturis.eu/l=eng/Thematic-disease-groups.html</a>.</p>
      <ul>
        <li><strong>Neurofibromatosis</strong>: Neurofibromatosis type 1 (NF1), Neurofibromatosis type 2 (NF2), Schwannomatosis</li>
        <li><strong>Lynch Syndrome and Polyposis</strong>: e.g. Familial Adenomatous Polyposis (FAP), MUTYH-Associated Polyposis (MAP), Polymerase Proofreading-Associated Polyposis (PPAP), NTHL1- Associated Polyposis (NAP), Peutz-Jeghers</li>
        <li><strong>Hereditary Breast and Ovarian Cancer</strong>: e.g. BRCA1, BRCA2, ATM, PALB2, CHEK2</li>
        <li><strong>More rare and predominantly malignant genetic tumour risk syndromes</strong>: e.g. PTEN Hamartoma Tumour Syndrome (PHTS), Li-Fraumeni, Birt-Hogg-Dubé, Familial Atypical Multiple Mole/Melanoma (FAMMM), CDH1-related Hereditary Diffuse Gastric Cancer (HDGC)</li>
      </ul>
      <img
        id="img-genturis-disease-groups"
        src="genturis-disease-groups.png"
        alt="thematic disease groups included in the genturis registry"
      />
      <p>The GENTURIS registry complies with the legal, ethical and privacy standards required within Europe. Patient data is protected in full compliance with the European Union (EU) regulations on the processing and free movement of personal data as well as with all pertinent national laws and regulations of the Member States. This includes the European Data Privacy Regulations and Good Clinical Practice guidelines.</p>
      <p>The centres participating in the GENTURIS registry have concluded an agreement on joint protection responsibility according to art. 26 of the General Data Protection Regulation (GDPR). Further information is available on request.</p>
      <p>Access to the GENTURIS registry data is regulated by the GENTURIS registry data access policy.</p>
    </PageSection>
    <PageSection id="genturis-section-committee" aria-labelledby="genturis-section-committee-title" width="large">
      <h2 id="genturis-section-committee-title">The GENTURIS registry steering committee</h2>
      <p>The GENTURIS registry steering committee is responsible for the daily management of the GENTURIS registry. The GENTURIS registry coordinator chairs the GENTURIS registry steering committee and the coordinator of ERN GENTURIS governs the GENTURIS registry.</p>
      <p>The GENTURIS registry steering committee consists of the following members.</p>
      <ul>
        <li>The coordinator of ERN GENTURIS</li>
        <li>The GENTURIS registry coordinator</li>
        <li>The GENTURIS registry data steward</li>
        <li>A representative of Task Force 5 Research</li>
        <li>A representative of software provider Molgenis</li>
        <li>An ERN GENTURIS European Patient Advocacy Group (ePAG) council representative</li>
      </ul>
    </PageSection>
    <PageSection id="genturis-section-partners" aria-labelledby="genturis-section-partners-title" width="large">
      <h2 id="genturis-section-partners-title">GENTURIS Registry Partners</h2>
      <p>The list of all participating Health Care Providers (HCPs) will be added in the near future.</p>
      <p>Access to the restricted area of the GENTURIS registry is only possible for collaborators from HCPs that are Full Member, Affiliated Partner or Supporting Partner of ERN GENTURIS, who have a GENTURIS registry login account.</p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
</script>

<style lang="scss">
#img-genturis-disease-groups {
  display: block;
  width: 90%;
  max-width: 812px;
  margin: 1.5em auto;
}
</style>
