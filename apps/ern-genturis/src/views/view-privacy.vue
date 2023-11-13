<template>
  <Page id="page-privacy-policy">
    <PageHeader
      class="genturis-header"
      title="Genturis Registry"
      subtitle="Privacy Policy"
      imageSrc="genturis-carousel.jpg"
      titlePositionX="center"
      titlePositionY="center"
    />
    <PageSection
      id="genturis-privacy-google"
      aria-labelledby="genturis-privacy-google-title"
    >
      <h2 id="genturis-privacy-google-title">Google analytics</h2>
      <p>
        This website uses Google Analytics, which is a analytics service
        provided by Google Inc. (“Google”). Please note that this website
        initializes Google Analytics with the setting anonymizeIp. This
        guarantees anonymized data collection by masking the last part of your
        IP address.
      </p>
      <p>
        Google will use this information for the purpose of evaluating your use
        of the website, compiling reports on website activity for the ERN
        GENTURIS project management team, and providing other services to the
        ERN GENTURIS project management team relating to website activity and
        internet usage. Google will not associate your IP address with any other
        data held by Google.
      </p>
      <p>
        You may refuse the use of cookies by selecting the appropriate settings
        on your browser, however please note that if you do this you may not be
        able to use the full functionality of this website. By using this
        website, you consent to the processing of data about you by Google in
        the manner and for the purposes set out above.
      </p>
      <p>
        You can prevent Google's collection and use of data (cookies and IP
        address) by downloading and installing the browser plug-in available
        under
        <a href="https://tools.google.com/dlpage/gaoptout?hl=en"
          >https://tools.google.com/dlpage/gaoptout?hl=en</a
        >.
      </p>
    </PageSection>
    <PageSection
      id="genturis-privacy-eservices"
      aria-labelledby="genturis-privacy-eservices-title"
    >
      <h2 id="genturis-privacy-eservices-title">E-Services</h2>
      <p>
        When you contact us by e-mail, your personal data is collected only to
        the extent necessary to reply. If the team responsible for the mailbox
        is unable to answer your question, or your questions has regards to
        another party, it might forward your email to another service.
      </p>
      <p>
        If you have any questions about the processing of your email and related
        personal data, do not hesitate to include them in your message.
      </p>
      <p>
        Our server, where all information is held, is located in The Netherlands
        meaning all the data and information we store is held within the EU.
      </p>
    </PageSection>
    <PageSection
      id="genturis-privacy-access"
      aria-labelledby="genturis-privacy-access-title"
    >
      <h2 id="genturis-privacy-access-title">
        Who has access to your information?
      </h2>
      <p>
        The collected personal data and all information related to the above
        mentioned consultation is stored on the e-mail server of the ERN
        GENTURIS Coordinating office at Radboudumc and potentially those who
        have been concerned in the correspondence in order to respond to your
        comment.
      </p>
      <p>
        We will not sell or rent your information to third parties. We will not
        share your information with third parties for marketing purposes.
      </p>
    </PageSection>
    <PageSection
      id="genturis-privacy-contact"
      aria-labelledby="genturis-privacy-contact-title"
    >
      <h2 id="genturis-privacy-contact-title">Contact</h2>
      <p>
        For any questions about the privacy policy of the GENTURIS registry,
        please contact the GENTURIS registry coordination team.
      </p>
      <p>
        You have the right to be forgotten and removed completely from the
        GENTURIS registry contact database. To request this, please contact the
        GENTURIS registry coordinator.
      </p>
      <GenturisAddress :centerText="true" :showEmail="true" />
    </PageSection>
    <PageSection
      id="genturis-privacy-links"
      aria-labelledby="genturis-privacy-links-title"
    >
      <h2 id="genturis-privacy-links-title">Third Party Links</h2>
      <p>
        This website includes links to third party website. By clicking on those
        links, third parties may collect or share your data. The GENTURIS
        registry is not responsible for the privacy policy of these third party
        websites and does not control these third party websites. Please read
        the privacy policy of these third party websites.
      </p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
</script>
