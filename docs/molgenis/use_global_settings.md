# Admin settings

You can find the 'admin' menu when on the MOLGENIS start page, when you view the list of databases (click the MOLGENIS
logo to get there). Only when signed in as 'admin' user will this menu item be shown.

Database settings currently supported:
* LANDING_PAGE - to change landing page from default /apps/central to something else
* locales - to enable internationalization (i18n), experimental. Should be javascript array, default ```["en"]```

Schema settings currently supported:
* menu - will be set by the 'settings' app on database and schema level
* pages - will be set by the pages app, access via settings app
* reports - will be set by the reports app

## User management

Currently you can use the admin menu to view the users currently registered in the system. In addition you can create a
new user and/or set a user password.
