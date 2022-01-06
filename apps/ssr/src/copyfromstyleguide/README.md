This is a copy from styleguide, and then changed to get rid of browser/node specific dependency on graphql (replace with
simple call)

Reason is that styleguide library as a whole is not yet ssr compliant.

This code is used to execute the graphql queries. Probably this code is much more then you need for this use case.