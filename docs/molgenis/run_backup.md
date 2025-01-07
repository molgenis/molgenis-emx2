# Backups 


Currently all data is stored in postgresql. In order to guarantee fast recovery after a hardware or software failure, we suggest to make daily backups.

## Backing up the database

Postgres comes with a variety of methods to backup and restore the state of your database. 
The ideal tool for handling backups of your database is the the 'pg_dump' utility which comes with PostgreSQL. 
We recommend setting up a cron job to do a pg_dump of your database every night


## Example 

Script below is making daily backups (max of 14 days) in the /var/molenis-backup folder.
You could use this script as a [cron.daily](https://en.wikipedia.org/wiki/Cron) script.


```
####
#
# Molgenis EMX2 local PGSQL backup. 
#       - pgsql backups on local server for local backup en restore usage
#       
# Restore can be done by: 
# su - postgres -c "zstdcat /var/molgenis-backup/posgresql-<<date>>.sql.zst | /usr/bin/psql"
###

USER="molgenis"

# Days to keep (minimal 4 for weekend overlap) increase for your setup
KEEP_DAYS=14

BACKUP_DIR="/var/molgenis-backup/"
CURRENT_DATE=$(date "+%d%m%Y")

if [ ! -d $BACKUP_DIR ]
then
        mkdir -p $BACKUP_DIR
        mkdir -p $BACKUP_DIR/TMP
        chgrp $USER $BACKUP_DIR
fi

# Cleanup Old backups
find $BACKUP_DIR/* -mtime +${KEEP_DAYS} -exec rm {} \;

# backup
sudo -u postgres pg_dump molgenis | zstd >> ${BACKUP_DIR}/postgresql-"${CURRENT_DATE}".sql.zst


```
 

