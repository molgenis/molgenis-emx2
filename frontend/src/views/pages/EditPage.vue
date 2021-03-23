<template>
  <div>
    <router-link :to="'/' + page">
      view page
    </router-link>
    <h1>{{ title }}</h1>
    <Spinner v-if="loading" />
    <div v-else>
      <MessageError v-if="graphqlError">
        {{ graphqlError }}
      </MessageError>
      <MessageSuccess v-if="success">
        {{ success }}
      </MessageSuccess>
      <ckeditor :key="page" v-model="draft" :config="editorConfig" />
      <div class="mt-2 float-right">
        <ButtonAction @click="savePage">
          Save '{{ page }}'
        </ButtonAction>
      </div>
    </div>
    <br>
    <br>
    <ShowMore title="debug">
      <pre>
page = {{ page }}

draft = {{ draft }}

session = {{ session }}
      </pre>
    </ShowMore>
  </div>
</template>

<script>
import CKEditor from 'ckeditor4-vue'
import {request} from 'graphql-request'
import {
  ButtonAction,
  MessageError,
  MessageSuccess,
  ShowMore,
} from '@/components/ui/index.js'

export default {
  components: {
    ButtonAction,
    MessageError,
    MessageSuccess,
    ShowMore,
    ckeditor: CKEditor.component,
  },
  props: {
    page: String,
    session: Object,
  },
  data() {
    return {
      draft: '<h1>New page</h1><p>Add your contents here</p>',
      editorConfig: {
        removeButtons: '',
        toolbar: [
          {
            groups: ['basicstyles', 'cleanup'],
            items: [
              'Bold',
              'Italic',
              'Underline',
              'Strike',
              'Subscript',
              'Superscript',
            ],
            name: 'basicstyles',
          },
          {
            groups: ['list', 'indent', 'blocks', 'align', 'bidi'],
            items: [
              'NumberedList',
              'BulletedList',
              '-',
              'Outdent',
              'Indent',
              '-',
              'Blockquote',
              '-',
              'JustifyLeft',
              'JustifyCenter',
              'JustifyRight',
              'JustifyBlock',
            ],
            name: 'paragraph',
          },
          {items: ['Link', 'Unlink', 'Anchor'], name: 'links'},
          {items: ['Image', 'SpecialChar'], name: 'insert'},
          {items: ['Format', 'Font', 'FontSize'], name: 'styles'},
          {items: ['Maximize'], name: 'tools'},
          {
            groups: ['mode'],
            items: ['Source'],
            name: 'document',
          },
        ],
      },
      graphqlError: null,
      loading: false,
      success: null,
    }
  },
  computed: {
    title() {
      if (
        this.session &&
        this.session.settings &&
        this.session.settings['page.' + this.page]
      )
        return 'Edit page \'' + this.page + '\''
      else return 'Create new page \'' + this.page + '\''
    },
  },
  watch: {
    session: {
      deep: true,
      handler() {
        this.reload()
      },
    },
  },
  created() {
    this.reload()
  },
  methods: {
    reload() {
      if (
        this.session &&
        this.session.settings &&
        this.session.settings['page.' + this.page]
      ) {
        this.draft = this.session.settings['page.' + this.page]
      } else {
        return 'New page, edit here'
      }
    },
    savePage() {
      this.loading = true
      this.graphqlError = null
      this.success = null
      request(
        'graphql',
        'mutation change($settings:[MolgenisSettingsInput]){change(settings:$settings){message}}',
        {
          settings: {
            key: 'page.' + this.page,
            value: this.draft.trim(),
          },
        },
      )
        .then((data) => {
          this.success = data.change.message
          this.session.settings['page.' + this.page] = this.draft
        })
        .catch((graphqlError) => {
          // eslint-disable-next-line no-console
          console.log(JSON.stringify(graphqlError))
          this.graphqlError = graphqlError.response.errors[0].message
        })
        .finally((this.loading = false))
    },
  },
}
</script>
