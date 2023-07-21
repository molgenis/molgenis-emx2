# Message api 

Endpoint to send message

endpoint: `/:schema/api/message/`

Api accepts a post request with application/json body, json object must contain:
```
{
    recipientsFilter: string,
    subject: string,
    body: string"
}
```

example:
```
{
    "recipientsFilter": "{\"filter\": {\"name\":{\"equals\":\"Test cohort\"}}}",
    "subject": "Say hello to my catalogue",
    "body": "Just wanted to say hello"
}
```

**recipientsFilter**: should be a valid emx2 gql filter
**subject**: human readable message subject
**body**: the message that needs to be send


**Message recipients**

The message receivers a determined by executing a query on the schema the resulting tree is flattened and all leaves are used as message recipients.

The query can be set via ```contactRecipientsQuery```
for example:

```
query Resources($filter:ResourcesFilter){ 
    Resources(filter: $filter) { 
        contacts { 
            email 
        }
   }
 }
```

## Sending email

Configurable settings

- EMAIL_HOST (default: "smtpout1.molgenis.net")
- EMAIL_PORT (default: "25")
- EMAIL_START_TTLS_ENABLE (default: "false")
- EMAIL_SSL_PROTOCOLS (default: "TLSv1.2")
- EMAIL_SOCKET_FACTORY_PORT (default: "")
- EMAIL_SOCKET_FACTORY_CLASS (default: "")
- EMAIL_SOCKET_FACTORY_FALLBACK (default: "")
- EMAIL_DEBUG (default: "false")
- EMAIL_AUTH (default: "false")
- EMAIL_SENDER_EMAIL (default: "no-reply@molgenis.net")
- EMAIL_SMTP_AUTHENTICATOR_SENDER_PASSWORD (default: null)

To test; set env vars and run sendmail test ( use gmail for single test). Molgenis prod setup works via internal network ( no auth, default settings )







