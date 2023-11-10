# Analytics

## Introduction

Currenty analytics only support [google analytics](https://analytics.google.com/analytics/web/)

Enable analytics for a schema by adding the following setting via the setting interface

key: `ANALYTICS_ID`

value `G-XXXXXXXXXX` ( add your MEASUREMENT-ID )


When the ANALYTICS_ID setting has a non-empty value, the user will be asked to accept the  analytics terms and conditions.

If the user accepts, the analytics script will be loaded and the user will be tracked.

### Change the cookie wall content

Update the cookie wall message by setting:

key: `ANALYTICS_COOKIE_WALL_CONTENT`

value: [a string or html string]

