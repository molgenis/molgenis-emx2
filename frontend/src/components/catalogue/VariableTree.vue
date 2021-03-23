<template>
  <div>
    {{ graphqlError }}
    <div class="row">
      <div class="col-3">
        <label>Topics:</label>
        <TopicFilter
          :selected="selectedTopic"
          :topics="topics"
          @deselect="deselect"
          @select="select"
        />
      </div>
      <VariablesList
        class="col-9"
        :resource-acronym="resourceAcronym"
        :topic="selectedTopic"
      />
    </div>
  </div>
</template>

<script>

import {request} from 'graphql-request'
import TopicFilter from './TopicSelector.vue'
import VariablesList from './VariablesList.vue'

export default {
  components: {
    TopicFilter,
    VariablesList,
  },
  props: {
    resourceAcronym: String,
  },
  data: function() {
    return {
      count: 0,
      graphqlError: null,
      limit: 20,
      loading: false,
      page: 1,
      search: '',
      selectedCollections: [],
      selectedTopic: null,
      success: null,
      timestamp: Date.now(),
      topics: [],
      variableSearch: '',
      variables: [],
    }
  },
  watch: {
    search() {
      this.applySearch(this.topics, this.search)
    },
  },
  created() {
    this.loadTopics()
  },
  methods: {
    applySearch(topics, terms) {
      let result = false
      topics.forEach((t) => {
        t.match = false
        if (
          terms == null ||
          t.name.toLowerCase().includes(terms.toLowerCase())
        ) {
          t.match = true
          result = true
        }
        if (t.childTopics && this.applySearch(t.childTopics, terms)) {
          t.match = true
          result = true
        }
      })
      return result
    },
    deselect() {
      this.selectedTopic = null
    },
    loadTopics() {
      request(
        'graphql',
        '{Topics(orderby:{order:ASC}){name,parent{name},variables_agg{count},children{name,variables_agg{count},children{name, variables_agg{count},children{name,variables{name},children{name,variables{name},children{name}}}}}}}',
      )
        .then((data) => {
          this.topics = data.Topics.filter(
            (t) => t['parentTopic'] == undefined,
          )
          this.topics = this.topicsWithContents(this.topics)
          this.applySearch(this.topics, this.search)
        })
        .catch((error) => {
          this.graphqlError = error.response.errors[0].message
        })
        .finally(() => {
          this.loading = false
        })
    },
    select(topic) {
      this.selectedTopic = topic.name
    },
    topicsWithContents(topics) {
      let result = []
      if (topics)
        topics.forEach((t) => {
          let childTopics = this.topicsWithContents(t.childTopics)
          if (t.variables || childTopics.length > 0) {
            result.push({
              childTopics: childTopics,
              name: t.name,
            })
          }
        })
      return result
    },
  },
}
</script>
