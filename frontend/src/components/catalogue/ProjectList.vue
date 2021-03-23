<template>
  <div>
    <MessageError v-if="graphqlError">
      {{ graphqlError }}
    </MessageError>
    <div>
      <Pagination
        v-if="count > 0"
        v-model="page"
        class="justify-content-center mb-2"
        :count="count"
        :default-value="page"
        :limit="limit"
      />
      <p v-else>
        No records found.
      </p>
    </div>
    <div class="row">
      <ProjectCard
        v-for="project in projects"
        :key="project.name"
        :project="project"
      />
    </div>
  </div>
</template>

<script>
import ProjectCard from './ProjectCard'
import {request} from 'graphql-request'
import {MessageError, Pagination} from '@mswertz/emx2-styleguide'

export default {
  components: {
    MessageError,
    Pagination,
    ProjectCard,
  },
  props: {
    filter: {
      type: Object,
      default() {
        return {}
      },
    },
    institutionAcronym: String,
    search: {
      String,
      default: '',
    },
  },
  data() {
    return {
      count: 0,
      graphqlError: null,
      limit: 20,
      loading: false,
      page: 1,
      projects: [],
    }
  },
  watch: {
    page() {
      this.reload()
    },
    search() {
      this.reload()
    },
  },
  created() {
    this.reload()
  },
  methods: {
    reload() {
      let searchString = ''
      if (this.search && this.search.trim() != '') {
        searchString = `search:"${this.search}",`
      }
      request(
        'graphql',
        `query Projects($filter:ProjectsFilter,$offset:Int,$limit:Int){Projects(offset:$offset,limit:$limit,${searchString}filter:$filter){name,acronym,type{name},description,website,provider{name}}
        ,Projects_agg(${searchString}filter:$filter){count}}`,
        {
          filter: this.filter,
          limit: this.limit,
          offset: (this.page - 1) * 10,
        },
      )
        .then((data) => {
          this.projects = data.Projects
          this.count = data.Projects_agg.count
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally(() => {
          this.loading = false
        })
    },
  },
}
</script>
