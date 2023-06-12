<template>
  <div class="breadcrumbs">
    <ul class="nav">
      <li class="home">
        <router-link :to="{name: 'home'}">
          <span class="visually-hidden">Home</span>
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" widht="21" height="21" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25" />
          </svg>

        </router-link>
      </li>
      <slot></slot>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'breadcrumbs-ui'
}
</script>

<style lang="scss">
.breadcrumbs {
  width: 100%;
  padding: 1em 0;
  
  .nav {
    display: flex;
    flex-direction: row;
    flex-wrap: nowrap;
    list-style: none;
    padding: 0;
    margin: 0;
    
    li {
      display: flex;
      justify-content: flex-start;
      align-items: center;
      
      &::after {
        content: '\003E';
        font-size: 11pt;
        margin: 0 8px;
        color: $gray-500;
      }

      &:last-child {
        &::after {
          content: '';
        }
      }
      
      a {
        text-decoration: none;
        border-bottom: 2px solid transparent;
        color: blue;
        
        &:hover, &:focus {
          border-bottom-color: blue;
        }
      }
    }
  }
}
</style>