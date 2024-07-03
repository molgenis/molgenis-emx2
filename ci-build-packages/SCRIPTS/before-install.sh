# create molgenis service group
getent group molgenis >/dev/null || groupadd -f molgenis

# create molgenis service user
if ! getent passwd molgenis >/dev/null ; then
      useradd -r -g molgenis -d /home/molgenis -s /sbin/nologin -c "Molgenis service account" molgenis
fi

