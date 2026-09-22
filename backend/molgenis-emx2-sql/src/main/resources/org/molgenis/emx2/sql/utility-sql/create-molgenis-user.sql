create user molgenis with login nosuperuser inherit createrole encrypted password 'molgenis';
grant all privileges on database molgenis to molgenis;