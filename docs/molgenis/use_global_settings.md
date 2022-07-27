# Admin settings

You can find the 'admin' menu when on the MOLGENIS start page, when you view the list of databases (click the MOLGENIS
logo to get there). Only when signed in as 'admin' user will this menu item be shown.

Settings currently supported:

* LANDING_PAGE - to change landing page from default /apps/central to something else

## User management

Currently you can use the admin menu to view the users currently registered in the system. In addition you can create a
new user and/or set a user password.

## Changelog 
A feature flag is used to enable the changelog feature
By adding a setting with key ```CHANGELOG_SCHEMAS``` to the system admin settings a list of schemas can set.
When a schema is created with a name that is on the ```CHANGELOG_SCHEMAS``` list, a changelog in created and maintained. 
The schema settings view has a tab that show a list of the changes.
