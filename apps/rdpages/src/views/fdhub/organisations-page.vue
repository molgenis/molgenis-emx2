<template>
  <Page>
    <PageHeader
      title="FDHub"
      subtitle="Find and add new organisations"
      imageSrc="/fdhub-header.jpg"
    />
    <PageSection aria-labelledby="welcome-title" :verticalPadding="2">
      <h2 id="welcome-title">ERN ReCONNET registry Documents</h2>
      <p>
        On this page, you can search for organisations in the
        <a href="https://ror.org/">ROR</a> registry and them to the <strong>Organistations</strong> ontology table.
        This allows you to standardise institution affiliations and save project specific metadata.
      </p>
      <form class="page-form" id="organisationSearch">
        <label for="orgsearch" class="input-label">
          <span>Search for an institution by name</span>
          <span class="input-description">
            E.g., Amsterdam, VUMC, etc.
          </span>
        </label>
        <div id="orgsearch-container"></div>
      </form>
      <MessageBox v-if="error">
        <p>Error retrieving records:</p>
        <span>{{ error }}</span>
      </MessageBox>
      <PageForm
        id="orgreview"
        title="Review Organisation"
        description="Review the information about the selected organisation and click confirm to add the record to the database."
        v-else-if="!error && Object.hasOwn(selection, 'id')"
      >
        <fieldset class="ror-meta">
          <legend>ROR Metadata</legend>
          <div>
            <label for="ror-id">Identifier</label>
            <input id="ror-id" v-model="selection.id" readonly />
          </div>
          <div>
            <label for="ror-code">Code</label>
            <input id="ror-code" v-model="selection.code" readonly />
          </div>
        </fieldset>
        <fieldset class="org-details">
          <legend>Organisation Details</legend>
          <div>
            <label for="ror-name">
              Name
              <span>Organisation name as defined by ROR</span>
            </label>
            <input id="ror-name" v-model="selection.name" readonly />
          </div>
          <div>
            <label for="ror-name-official">
              Project Offical Name
              <span>
                Enter the official name specified by the relevant project
              </span>
            </label>
            <input
              id="ror-name-official"
              v-model="selection.projectOfficialName"
            />
          </div>
          <div>
            <label for="ror-department">
              Department
              <span
                >Enter a sub-institutional affiliation; e.g., Department of
                Genetics</span
              >
            </label>
            <input id="ror-department" v-model="selection.department" />
          </div>
          <div>
            <label for="ror-alt-ids">
              Alternative Identifiers
              <span>Enter one or more alternative identifiers</span>
            </label>
            <input
              id="ror-alt-ids"
              v-model="selection.alternativeIdentifiers"
            />
          </div>
          <div>
            <label for="ror-name-alt">
              Aliases
              <span>Additional names stored in ROR</span>
            </label>
            <input id="ror-name-alt" v-model="selection.aliases" readonly />
          </div>
        </fieldset>
        <fieldset class="org-location">
          <legend>Location</legend>
          <div>
            <label for="ror-country">Country</label>
            <input
              id="ror-country"
              v-model="selection.country.country_name"
              readonly
            />
          </div>
          <div>
            <label for="ror-country-code">Country Code</label>
            <input
              id="ror-country-code"
              v-model="selection.country.country_code"
              readonly
            />
          </div>
          <div>
            <label for="ror-city">City</label>
            <input
              id="ror-city"
              v-model="selection.addresses[0].city"
              readonly
            />
          </div>
          <div>
            <label for="ror-lat">Latitude</label>
            <input id="ror-lat" v-model="selection.addresses[0].lat" readonly />
          </div>
          <div>
            <label for="ror-lng">Longitude</label>
            <input id="ror-lng" v-model="selection.addresses[0].lng" readonly />
          </div>
        </fieldset>
        <fieldset class="org-contact">
          <legend>Point of Contact</legend>
          <div>
            <label for="ror-contact-email">Email</label>
            <input id="ror-contact-email" v-model="selection.contact.email"/>
          </div>
          <div>
            <label for="ror-first-name">First name</label>
            <input id="ror-first-name" v-model="selection.contact.firstName"/>
          </div>
          <div>
            <label for="ror-last-name">Last name</label>
            <input id="ror-last-name" v-model="selection.contact.lastName"/>
          </div>
          <div>
            <label for="ror-role">Role</label>
            <input id="ror-role" v-model="selection.contact.role"/>
          </div>
        </fieldset>
        <fieldset class="org-extra">
          <legend>Additional Information</legend>
          <div>
            <label for="ror-comment">
              Comment
              <span>
                Enter any additional information or comments about this
                organisation
              </span>
            </label>
            <textarea id="ror-comment" />
          </div>
        </fieldset>
        <div class="form-controls">
          <ButtonAction @click="onClick"> Save </ButtonAction>
        </div>
      </PageForm>
    </PageSection>
    <PageSection>
      <h2>ROR API Response + Schema</h2>
      <output class="json-output">
        {{ selection }}
      </output>
    </PageSection>
  </Page>
</template>

<script setup>
import { ref, onMounted } from "vue";
import {
  Page,
  PageHeader,
  PageSection,
  MessageBox,
  PageForm,
} from "molgenis-viz";
import { ButtonAction } from "molgenis-components";
import accessibleAutocomplete from "accessible-autocomplete";

let results = ref([]);
let selection = ref({});
let error = ref(null);

async function fetchData(url) {
  const response = await fetch(url);
  if (response.status / 100 !== 2) {
    const error = JSON.stringify({
      message: response.statusText,
      status: response.status,
      url: response.url,
    });
    throw new Error(error);
  }
  return response.json();
}

function searchRor(query, populateResults) {
  selection.value = {};
  const url = `https://api.ror.org/organizations?query=${encodeURI(query)}`;
  Promise.resolve(fetchData(url))
    .then((response) => {
      const data = response.items;
      const options = data.map((row) => row.name);
      results.value = data;
      populateResults(options);
    })
    .catch((error) => {
      error.value = error;
    });
}

function retrieveRorRecord(value) {
  let org = results.value.filter((row) => row.name == value)[0];
  org.department = "";
  org.alternativeIdentifiers = "";
  org.comment = "";
  org.code = org.id.split("/").reverse()[0];
  org.aliases = org.aliases.length ? org.aliases.join(',') : '';
  org.contact = {email: "", firstName: "", lastName: "", role: ""};
  selection.value = org;
}

function onClick() {
  alert("Data will be saved to table when this is built.");
}

onMounted(() => {
  accessibleAutocomplete({
    element: document.querySelector("#orgsearch-container"),
    id: "orgsearch",
    source: searchRor,
    confirmOnBlur: false,
    minLength: 3,
    onConfirm: retrieveRorRecord,
  });
});
</script>

<style lang="scss">
@import "accessible-autocomplete";

#organisationSearch {
  box-shadow: none;
  padding: 0;
}

#orgreview {
  background-color: $gray-050;
  margin-top: 2em;
  box-shadow: none;

  .form-sections {
    input {
      display: block;
      width: 100%;
      font-size: 12pt;

      &:read-only {
        background-color: $blue-050;
        color: $gray-700;
      }
    }

    fieldset {
      display: grid;
      gap: 0.5em 1.5em;
      margin-top: 1.25em;

      legend {
        font-size: 16pt;
        margin-bottom: 0.4em;
      }

      div {
        flex-grow: 1;
        label {
          display: block;
          margin-bottom: 0.2em;
          font-size: 13pt;

          span {
            display: block;
            color: $gray-600;
            font-size: 11pt;
          }
        }

        textarea {
          width: 100%;
        }
      }

      &.ror-meta {
        grid-template:
          "Title Title"
          "ID Code";
          
          legend {
            grid-area: Title;
          }
          
          &:nth-child(2) {
            grid-area: ID;
          }
          
          &:nth-child(3) {
            grid-area: Code;
          }
      }

      &.org-details {
        grid-template:
          "Title Title"
          "RorName OfficialName"
          "Department Department"
          "Identifiers Aliases"
          ;
          
          legend {
            grid-area: Title;
          }
          
          :nth-child(2) {
            grid-area: RorName;
          }

          :nth-child(3) {
            grid-area: OfficialName;
          }

          :nth-child(4) {
            grid-area: Department;
          }

          :nth-child(5) {
            grid-area: Identifiers;
          }
         
          :nth-child(6) {
            grid-area: Aliases;
          }
      }

      &.org-location {
        grid-template:
          "Title Title Title"
          "City Country CountryCode"
          "Latitude Longitude EMPTY";
          
          legend {
            grid-area: Title;
          }
          
          :nth-child(2) {
            grid-area: City;
          }
          
          :nth-child(3) {
            grid-area: Country;
          }
          
          :nth-child(4) {
            grid-area: CountryCode;
          }
          
          :nth-child(5) {
            grid-area: Latitude;
          }
          :nth-child(6) {
            grid-area: Longitude;
          }
      }
      
      &.org-contact {
        grid-template: 
          "Title Title"
          "Email Email"
          "FirstName LastName"
          "Role EMPTY";
        
        legend {
          grid-area: Title;
        }
        
        :nth-child(2) {
          grid-area: Email;
        }
        
        :nth-child(3) {
          grid-area: FirstName;
        }
        
        :nth-child(4) {
          grid-area: LastName;
        }
        
        :nth-child(5) {
          grid-area: Role;
        }
      }
    }
  }

  .form-controls {
    margin-top: 1em;
    display: flex;
    justify-content: flex-end;

    button {
      width: 150px;
    }
  }
}

.json-output {
  display: block;
  box-sizing: content-box;
  padding: 1em;
  background-color: $gray-050;
  white-space: pre;
}
</style>
