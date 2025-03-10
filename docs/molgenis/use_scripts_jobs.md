# Scripts and Jobs

In the 'tasks' menu item (also available via http://servername/apps/tasks) you can define scripts and view jobs. Under the hood, in SYSTEM database suitable
tables for Scripts and Jobs

## Scripts

Scripts currently can only be python scripts.

Scripts can have the following:

* unique name, required
* the script type
* the script
* outputFileExtension, optional, for returning output files. E.g. 'txt'
* disabled, when set to true the script will not run if a cron schedule is set
* cron, will schedule the script to run at planned intervals
* failureAddress, when set, will send a message to this email address, if a job fails

Your script will receive as environment variables:

* a token via MOLGENIS_TOKEN
* a path to OUTPUT_FILE which you can use to produce an outputFile which will then be stored in Jobs.output

### Pyclient

Python scripts can make use of the [Molgenis Pyclient](use_usingpyclient.md). Make sure to initialise the client as
described [here](use_usingpyclient.md#scripts-and-jobs)


## Jobs

Lists the previous jobs. You can see the progress on each of them. Also you can expect any output produced.

## API

Using the MOLGENIS_TOKEN you can also use API to submit jobs (change server URL to your server):

`
curl -X POST https://localhost:8080/api/tasks/ -v
`

or with parameters
`
curl -X POST https://localhost/api/tasks -v -d 'my parameters, as text or json or whever you like'  
`

You will receive a taskID, you can use that to inspect status of the running task

E.g. to retrieve task 123123:
`
curl GET https://server/api/tasks/123123
`