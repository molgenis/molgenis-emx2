if [ "$1" != "1" ] # Don't execute during upgrade
then
  rm -f /etc/systemd/system/molgenis-emx2|| true
  rm -f /var/log/molgenis/molgenis-emx2*|| true
fi
