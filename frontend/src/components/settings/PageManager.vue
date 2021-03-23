<template>
  <div>
    <h5 class="card-title">
      Manage pages
    </h5>
    <ul>
      <li v-for="page in pages">
        <a :href="'../pages/#/' + page">{{ page }}</a>
        <IconAction icon="edit" @click="openPageEdit(page)" />
      </li>
    </ul>
    <form class="form-inline">
      <InputString
        v-model="newPage"
        :error-message="nameError"
        label="Add new page: "
      />
      <ButtonAction
        v-if="newPage && !nameError" @click="openPageEdit(newPage)"
      >
        Create new
      </ButtonAction>
    </form>
  </div>
</template>

<script>
import {ButtonAction, IconAction, InputString} from '@/components/ui/index.js'

export default {
  components: {
    ButtonAction,
    IconAction,
    InputString,
  },
  props: {
    session: Object,
  },
  data() {
    return {
      newPage: null,
    }
  },
  computed: {
    // eslint-disable-next-line vue/return-in-computed-property
    nameError() {
      if (this.pages.includes(this.newPage)) {
        return 'Page name already exists'
      }
    },
    pages() {
      if (this.session && this.session.settings) {
        return Object.keys(this.session.settings)
          .filter((key) => key.startsWith('page.'))
          .map((key) => key.substring(5))
      }
      return []
    },
  },
  methods: {
    openPageEdit(page) {
      window.open('../pages/#/' + page + '/edit', '_blank')
    },
  },
}
</script>
