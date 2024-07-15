<template>
  <Page>
    <PageHeader
      title="Local GDI Portal"
      subtitle="Get Started with the Local Portal"
      imageSrc="gdi-portal.jpg"
      height="large"
      titlePositionX="center"
    />
    <PageSection
      id="section-about"
      aria-labelledby="section-about-title"
      :verticalPadding="2"
    >
      <h2 id="section-about-title">About the local portal</h2>
      <p>
        The GDI local portal implementation runs on
        <a href="https://www.molgenis.org">MOLGENIS</a> (Java, PostgreSQL,
        GraphQL) and is connected to
        <a href="https://github.com/CSCfi/rems">REMS</a>. Metadata is also
        exposed through the
        <a href="https://www.fairdatapoint.org">Fair Data Points</a> and
        <a href="https://beacon-project.io">Beacon</a> endpoints, which allows
        the local portal to connect with other networks. By default, OIDC is
        enabled to allow users to seamlessly sign in to these systems using a
        single account.
      </p>
    </PageSection>
    <PageSection>
      <h2>Managing the local portal</h2>
      <p>
        Most of the configurations are completed during the setup process, but
        other steps (i.e., data import) requires a few extra steps. In this
        section, you will find information on how to manage your local portal.
      </p>
      <h3>Importing data</h3>
      <p>
        Before you begin, we would like to introduce you to the MOLGENIS concept
        of <em>schemas</em> and <em>tables</em>. In MOLGENIS EMX2, data models
        are written in a csv or xlsx file. This file contains the definitions of
        all the attributes (i.e., columns) you would like to use and
        instructions on how those attributes are structured within the model
        (i.e., as tables). In a sense, a schema is essentially an instance of
        data model or, in otherwords, it is a database. It is important to be
        aware of this as you are importing data into the portal as you will need
        to know which schema and table you are editing.
      </p>
      <p>
        Data can be entered directly into the local portal as individual records
        or as a bulk uploaded. The process for manual entry and bulk upload is
        outlined below.
      </p>
      <h4>Manual entry</h4>
      <p>
        Manual entry may be useful if you would like to add or edit a record in
        the browser.
      </p>
      <ol>
        <li>
          To enter a record manually, navigate to the
          <a href="../tables/#/">Tables</a> view.
        </li>
        <li>
          Find the table that you would like to add a record to (e.g.,
          <a href="../tables/#/Individuals">Individuals</a>) and open it.
        </li>
        <li>
          In the top left corner of the data table, click the plus "<PlusIcon
            class="heroicons"
            style="width: 13pt"
          />" icon.
        </li>
        <li>
          This will open the "create a new record" form. Fill in the required
          fields and any other relevant information. When you have finished,
          save the record to the table. You can also save the new entry as a
          draft. This allows you to finish the editing the form at a later
          point.
        </li>
      </ol>
      <h4>File Upload</h4>
      <p>
        Data can be imported into MOLGENIS in the following formats: csv, tsv,
        xlsx, and csv.zip. If you would like to create a template, you can
        download the table structure from the
        <a href="../updownload">Upload and Download</a>
        page. (Note: You may need to erase any data in the file if the table
        isn't empty.)
      </p>
      <ol>
        <li>In your local file, enter one or more records and save.</li>
        <li>
          Return to the <a href="../updownload">Upload and Download</a> page and
          click the "Browse" button. Select the file you would like to import
          and click the "Import" button.
        </li>
      </ol>
      <p>
        Any import errors will be displayed on the page. If you are importing a
        csv file, make sure the name of the file matches the name of the table.
        The names can be found in the <a href="../tables/#/">Tables</a> view.
        Make sure the sheet name is the same as the table if you are importing
        an xlsx file.
      </p>
    </PageSection>
  </Page>
</template>

<script setup>
import { Page, PageHeader, PageSection } from "molgenis-viz";
import { PlusIcon } from "@heroicons/vue/24/outline";
</script>
