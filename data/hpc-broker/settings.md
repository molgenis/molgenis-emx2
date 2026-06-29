### Setting for sending message
`contactRecipientsQuery`

```
query Jobs($filter:JobsFilter) { 
   Jobs(filter: $filter) {
      email 
   }
 }
```