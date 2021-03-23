<template>
  <div class="container bg-white">
    <MessageError v-if="graphqlError">
      {{ graphqlError }}
    </MessageError>
    <div v-if="variable">
      <div class="p-2 bg-dark text-white mb-3">
        <h6>
          <RouterLink class="text-white" to="/">
            home
          </RouterLink>
          /
          <RouterLink class="text-white" to="/list/Variables">
            variables
          </RouterLink>
          /
        </h6>
      </div>
      <h6 class="d-inline">
        {{ resourceType }}&nbsp;
      </h6>
      <RouterLink
        :to="{
          name: resourceType.toLowerCase(),
          params: {
            acronym: variable.release.resource.acronym,
          },
        }"
      >
        {{ variable.release.resource.acronym }}
      </RouterLink>
      /
      <h6 class="d-inline">
        Release
      </h6>
      <RouterLink
        :to="{
          name: 'release',
          params: {
            acronym: variable.release.resource.acronym,
            version: variable.release.version,
          },
        }"
      >
        {{ variable.release.version }}
      </RouterLink>
      /
      <h6 class="d-inline">
        Table name
      </h6>
      <RouterLink
        :to="{
          name: 'table',
          params: {
            acronym: variable.release.resource.acronym,
            name: variable.table.name,
            version: variable.release.version,
          },
        }"
      >
        {{ variable.table.name }}
      </RouterLink>

      <h1>Variable: {{ variable.name }}</h1>
      <div class="row">
        <div class="col">
          <h6>Variable name</h6>
          <p>{{ variable.name }}</p>
          <h6>Variable type</h6>
          <OntologyTerms color="dark" :terms="[variable.format]" />
          <h6>Variable topics</h6>
          <OntologyTerms color="dark" :terms="variable.topics" />
          <h6>Variable unit</h6>
          <OntologyTerms color="dark" :terms="[variable.unit]" />
          <h6>Mandatory</h6>
          <p>{{ variable.mandatory ? "YES" : "NO" }}</p>
          <h6>Variable Description</h6>
          <p>{{ variable.description ? variable.description : "N/A" }}</p>
          <h6>Vocabularies</h6>
          <OntologyTerms color="dark" :terms="variable.vocabularies" />

          <h6>Permitted values</h6>
          <table
            v-if="variable.permittedValues"
            class="table table-sm table-bordered m-2"
          >
            <thead>
              <th>value</th>
              <th>label</th>
              <th>isMissing</th>
            </thead>
            <tbody>
              <tr v-for="v in variable.permittedValues">
                <td>{{ v.value }}</td>
                <td>{{ v.label }}</td>
                <td>
                  {{ v.isMissing ? "YES" : "NO" }}
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else>
            N/A
          </p>
          <p v-else>
            N/A
          </p>
          <h6>Example values</h6>
          <ul v-if="variable.exampleValues">
            <li v-for="v in variable.exampleValues" :key="v">
              '{{ v }}'
            </li>
          </ul>
          <p v-else>
            N/A
          </p>
          <h6>This variable is repeated as:</h6>
          <table
            v-if="variable.repeats"
            class="table table-sm table-bordered m-2"
          >
            <thead>
              <th>table</th>
              <th>name</th>
              <th>collectionEvent</th>
            </thead>
            <tbody>
              <tr v-for="v in variable.repeats">
                <td>{{ v.table.name }}</td>
                <td>{{ v.name }}</td>
                <td>
                  {{ v.collectionEvent ? v.collectionEvent.name : "N/A" }}
                </td>
              </tr>
            </tbody>
          </table>
          <p v-else>
            N/A
          </p>
        </div>
        <div class="col" />
      </div>

      <h6>Mappings</h6>
      <table v-if="variable.mappings" class="table table-sm table-bordered">
        <thead>
          <tr>
            <th>fromTable</th>
            <th>fromVariables</th>
            <th>match</th>
            <th>description</th>
            <th>syntax</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in variable.mappings">
            <td>
              <RouterLink
                :to="{
                  name: 'tablemapping',
                  params: {
                    fromAcronym: m.fromRelease.resource.acronym,
                    fromVersion: m.fromRelease.version,
                    fromTable: m.fromTable.name,
                    toAcronym: variable.release.resource.acronym,
                    toVersion: variable.release.version,
                    toTable: variable.table.name,
                  },
                }"
              >
                {{ getType(m.fromRelease.resource.mg_tableclass) }}:
                {{ m.fromRelease.resource.acronym }}
                {{ m.fromRelease.version }}, table: {{ m.fromTable.name }}
              </RouterLink>
            </td>

            <td>
              <div v-for="v in m.fromVariable" :key="v.name">
                <RouterLink
                  :to="{
                    name: 'tablemapping',
                    params: {
                      acronym: m.fromRelease.resource.acronym,
                      version: m.fromRelease.version,
                      table: m.fromTable.name,
                      name: v.name,
                    },
                  }"
                >
                  {{ v.name }}
                </RouterLink>
              </div>
            </td>
            <td>{{ m.match.name }}</td>
            <td>{{ m.description }}</td>
            <td>
              <pre>{{ m.syntax }}</pre>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else>
        N/A
      </p>
    </div>
  </div>
</template>

<script>
import {MessageError} from '@/components/ui/index.js'
import OntologyTerms from '@/components/catalogue/OntologyTerms.vue'
import {request} from 'graphql-request'

export default {
  components: {
    MessageError,
    OntologyTerms,
  },
  props: {
    acronym: String,
    name: String,
    table: String,
    version: String,
  },
  data() {
    return {
      graphqlError: null,
      variable: null,
    }
  },
  computed: {
    // eslint-disable-next-line vue/return-in-computed-property
    resourceType() {
      if (this.variable.release) {
        return this.getType(this.variable.release.resource.mg_tableclass)
      }
    },
  },
  created() {
    this.reload()
  },
  methods: {
    getType(mg_tableclass) {
      return mg_tableclass.split('.')[1].slice(0, -1)
    },
    reload() {
      request(
        'graphql',
        `query Variables($acronym:String,$version:String,$table:String,$name:String){Variables(filter:{release:{version:{equals:[$version]},resource:{acronym:{equals:[$acronym]}}},table:{name:{equals:[$table]}},name:{equals:[$name]}})
        {name,table{name},repeats{name,table{name},collectionEvent{name}},format{name},vocabularies{name,definition,ontologyTermURI},mandatory,unit{name,definition,ontologyTermURI},exampleValues,permittedValues{value,label,isMissing},release{version,resource{acronym,name,mg_tableclass}},description,label,topics{name,ontologyTermURI,definition}
                mappings{description,syntax,match{name}fromTable{name}fromVariable{name}fromRelease{resource{acronym,mg_tableclass}version}}}}`,
        {
          acronym: this.acronym,
          name: this.name,
          table: this.table,
          version: this.version,
        },
      )
        .then((data) => {
          this.variable = data.Variables[0]
        })
        .catch((error) => {
          if (error.response)
            this.graphqlError = error.response.errors[0].message
          else this.graphqlError = error
        })
        .finally(() => {
          this.loading = false
        })
    },
  },
}
</script>
