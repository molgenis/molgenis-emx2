# Scripts and Jobs

In the 'tasks' menu item (also available via http://servername/apps/tasks) you can define scripts and view jobs. Under the hood, in ADMIN database suitable
tables for Scripts and Jobs

## Scripts

Scripts currently can only be python scripts.

Scripts can have the following:

* unique name, required
* the script type
* the script
* outputFileExtension, optional, for returning output files. E.g. 'txt'
* disabled, when true scripts cannot be run
* cron, will schedule the script to run at planned intervals

Your script will receive as environment variables:

* a token via MOLGENIS_TOKEN
* a path to OUTPUT_FILE which you can use to produce an outputFile which will then be stored in Jobs.output

## Jobs

Lists the previous jobs. You can see the progress on each of them. Also you can expect any output produced.

## API

Using the MOLGENIS_TOKEN you can also use API to submit jobs:

`https:/`


