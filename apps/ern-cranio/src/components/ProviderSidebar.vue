<template>
  <aside class="sidebar">
    <h2>Dashboards</h2>
    <nav>
      <ul class="navlinks">
        <li>
          <router-link
            :to="{
              name: 'provider-overview',
              params: { id: organization.id },
            }"
            @click="onClick"
          >
            Your center
          </router-link>
        </li>
        <li>
          <router-link
            :to="{
              name: 'provider-cranio',
              params: { id: organization.id },
            }"
            @click="onClick"
          >
            Craniosynostosis
          </router-link>
        </li>
        <li>
          <router-link
            :to="{
              name: 'provider-clp',
              params: { id: organization.id },
            }"
            @click="onClick"
          >
            Cleft lip and palate
          </router-link>
        </li>
        <li>
          <router-link
            :to="{
              name: 'provider-genetic-deafness',
              params: { id: organization.id },
            }"
            @click="onClick"
          >
            Genetic Deafness
          </router-link>
        </li>
        <li>
          <router-link
            :to="{
              name: 'provider-larnyxcleft',
              params: { id: organization.id },
            }"
            @click="onClick"
          >
            Larynxcleft
          </router-link>
        </li>
      </ul>
    </nav>
    <ButtonAction id="btnSubmitPatient">
      <span>Submit patient</span>
      <PlusCircleIcon />
    </ButtonAction>
  </aside>
</template>

<script setup>
import { ButtonAction } from "molgenis-components";
import { PlusCircleIcon } from "@heroicons/vue/24/outline";

const props = defineProps({
  user: String,
  organization: Object,
});

function onClick(event) {
  document
    .querySelectorAll(".navlinks li")
    .forEach((elem) => elem.classList.remove("link-selected"));
  const elem = event.target.parentNode;
  elem.classList.add("link-selected");
}
</script>

<style lang="scss">
.sidebar {
  background-color: $gray-000;
  box-sizing: content-box;
  padding: 2em;
  box-shadow: $box-shadow;
  border-radius: 8px;
  max-width: 300px;

  nav {
    padding: 0;
    margin: 2em 0;

    .navlinks {
      list-style: none;
      padding: 0;
      width: 100%;

      li {
        border-left: 5px solid transparent;
        margin-bottom: 1em;
        padding: 0.4em;
        padding-left: 1.5em;

        a {
          color: currentColor;

          &:hover,
          &:focus {
            color: var(--primary);
          }
        }

        &.link-selected {
          border-left-color: var(--primary);
          a {
            color: var(--primary);
          }
        }
      }
    }
  }
}

#btnSubmitPatient {
  width: 100%;
  padding: 0.7em;

  svg {
    width: 21px;
    margin-left: 4px;
    margin-top: -2px;
    path {
      stroke-width: 2;
    }
  }
}
</style>
