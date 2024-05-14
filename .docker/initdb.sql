/* use this init script if you want to test EMX2 with non-molgenis user */
CREATE DATABASE molgenis;
ALTER DATABASE molgenis SET jit = 'off';
CREATE USER molgenis WITH LOGIN NOSUPERUSER INHERIT CREATEROLE ENCRYPTED PASSWORD 'molgenis';
GRANT ALL PRIVILEGES ON DATABASE molgenis TO molgenis;