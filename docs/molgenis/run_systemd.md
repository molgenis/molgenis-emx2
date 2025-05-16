
# Install Molgenis emx2 as a system service 
We support Redhat and Ubuntu based installations.


## Java 

MOLGENIS EMX2 runs on java ≥ 21
<!-- tabs:start -->

#### **Ubuntu (apt)**
```console
sudo apt -y install openjdk-21-jre-headless
```

#### **Redhat (yum)**

```console
sudo yum install java-21-openjdk-headless

```
<!-- tabs:end -->


## Postgresql 


MOLGENIS EMX2 depends on Postgresql 15
Please see https://www.postgresql.org/download/ for your supported OS version. EMX2 is supported on version 15! 
For Redhat and Ubuntu you can use below information:


<!-- tabs:start -->

#### **Ubuntu (apt)**
```console
sudo install -d /usr/share/postgresql-common/pgdg
sudo curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc

sudo sh -c 'echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" > /etc/apt/sources.list.d/pgdg.list'

sudo apt update

sudo apt install postgresql-15 postgresql-contrib-15 postgresql-client-15 

```

#### **Redhat (yum)**

```console
sudo dnf install -y https://download.postgresql.org/pub/repos/yum/reporpms/EL-9-x86_64/pgdg-redhat-repo-latest.noarch.rpm


sudo dnf -qy module disable postgresql


sudo dnf install -y postgresql15-server postgresql15-contrib

sudo /usr/pgsql-15/bin/postgresql-15-setup initdb
sudo systemctl enable postgresql-15
sudo systemctl start postgresql-15


```
<!-- tabs:end -->



## Database Creation 

```console
sudo -u postgres psql
```


```console

create database molgenis;
alter database molgenis SET jit = 'off';
create user molgenis with login nosuperuser inherit createrole encrypted password 'molgenis';
grant all privileges on database molgenis to molgenis;
exit;

```
Note: 
If you change the database password you must edit the password after the installation of molgenis-emx2 in the /etc/systemd/system/molgenis-emx2.service





## Add molgenis-emx2 Repo


Add molgenis-emx2 repo and install molgenis-emx2
<!-- tabs:start -->

#### **Ubuntu (apt)**
```console

wget -O- https://registry.molgenis.org/repository/packages/apt/molgenis-public.gpg | sudo gpg --yes --dearmor -o /usr/share/keyrings/molgenis-keyring.gpg

echo "deb [signed-by=/usr/share/keyrings/molgenis-keyring.gpg] https://registry.molgenis.org/repository/molgenis-emx2-$(lsb_release -cs)/ $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/molgenis.list

```

#### **Redhat (yum)**

```console
sudo cat > /etc/yum.repos.d/molgenis-emx2.repo <<EOL
[molgenisemx2repo]
name=Molgenis Emx2 Repository
baseurl=https://registry.molgenis.org/repository/molgenis-emx2-yum
enabled=1
gpgcheck=0
gpgkey=file:///etc/pki/rpm-gpg/RPM-GPG-KEY-CentOS-7
repo_gpgcheck=0
priority=1
EOL

```
<!-- tabs:end -->

## Installation


<!-- tabs:start -->

#### **Ubuntu (apt)**
```console
sudo apt install molgenis-emx2 
```

#### **Redhat (yum)**

```console
sudo yum install molgenis-emx2 

```
<!-- tabs:end -->




## Application emx2 

Molgenis is running port 8080. 
* Open on http://ip.or.host.of.server:8080

In most cases you can proxy_pass traffic to molgenis-emx2 with a (hard or software) loadbalancer to the molgenis-emx2 (port 8080) endpoind.
Or you can use nginx to proxypass traffic.

An example of nginx proxy pass:

```nginx
location / {
    proxy_pass http://localhost:8080;
    client_max_body_size 0;
    proxy_read_timeout 600s;
    proxy_redirect http://localhost:8080/ $scheme://$host/;
    proxy_set_header Host $host;
    proxy_http_version 1.1;
}
```

> Note: the above is still not secure. To secure the communication using https and nginx you should put an [ssl](https://www.thesslstore.com/knowledgebase/ssl-install/nginx-ssl-installation/) config 


## Optional
Optionally, you can change defaults using either java properties or using env variables in the /etc/systemd/system/molgenis-emx2.service 

* MOLGENIS_POSTGRES_URI
* MOLGENIS_POSTGRES_USER
* MOLGENIS_POSTGRES_PASS
* MOLGENIS_HTTP_PORT
* MOLGENIS_ADMIN_PW



## Troubleshooting

Check if EMX2 is running

```console
systemctl status molgenis-emx2
```


Check if postgresql is running

```console
systemctl status postgresql
```

Debugging log files:
/var/log/molgenis
 * emx2.log
 * emx2-error.log

