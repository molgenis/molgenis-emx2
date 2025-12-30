#!/bin/bash 

VERSION=$1
RELEASE=1
NAME=molgenis-emx2
SUMMARY="The world's most customizable platform for (scientific) data and FAIR principles (findability, accessibility, interoperability and reusability)."
BUILD_ARCH=noarch
LICENSE=LGPL3
USER=molgenis
NEXUS_USER=$2
NEXUS_PASS=$3


if [ ! "$VERSION" ]; then
  echo "You need to specify the stable version like bash create-stable-release.sh 10.66.1 registry_user registry_pass"
  exit 1;
fi

if [ $4 ]; then
  #for building package increase options 
  RELEASE=$4 
fi

check_req() {
  for COMMAND in "wget" "curl" "fpm" "rpmbuild" "dpkg-deb"; do
    command_exists "${COMMAND}"
  done
    
}   

command_exists() {
  # check if command exists and fail otherwise
  command -v "$1" >/dev/null 2>&1
  if [ $? -ne 0 ]; then
      echo "I require $1 but it's not installed. Abort."
      exit 1
  fi
}

create_buildroot() {

  # First remove old buildroot if exists
  if [ -d buildroot ]; then
    rm -Rf buildroot
  fi
  
  # First create the buildroot
  mkdir -p buildroot/usr/local/share/molgenis
  mkdir -p buildroot/var/log/molgenis/
  mkdir -p buildroot/etc/systemd/system/
  
  
  #Copy files to the buildroot
  cp log4j2.xml buildroot/usr/local/share/molgenis/
  cp molgenis-emx2.service buildroot/etc/systemd/system/
  
  
  # Downloading version to buildroot
  
  echo "Downloading molgenis-emx2-$VERSION-all.jar from github\n"
  wget -q -O buildroot/usr/local/share/molgenis/molgenis-emx2-$VERSION-all.jar https://github.com/molgenis/molgenis-emx2/releases/download/v$VERSION/molgenis-emx2-$VERSION-all.jar 
  pushd .
  cd buildroot/usr/local/share/molgenis
  ln -s molgenis-emx2-$VERSION-all.jar molgenis-emx2.jar
  popd

}


create_builds() {
echo "Creating RPM"
# RPM Creation
fpm -t rpm -s dir \
        -f -n "$NAME" -v "$VERSION" --iteration "$RELEASE" -a "$BUILD_ARCH" \
        --description "$SUMMARY" --url "$WEB_URL" --license "$LICENSE" --vendor "$VENDOR" \
        --depends "tar" \
        --before-install SCRIPTS/before-install.sh \
        --after-install SCRIPTS/after-install.sh \
        --before-remove SCRIPTS/before-remove.sh \
        --after-remove SCRIPTS/after-remove.sh \
        -C buildroot .
#DEB Creation
echo "Creating DEB"
fpm -t deb -s dir \
        -f -n "$NAME" -v "$VERSION" --iteration "$RELEASE" -a "$BUILD_ARCH" \
        --description "$SUMMARY" --url "$WEB_URL" --license "$LICENSE" --vendor "$VENDOR" \
        --depends "tar" \
        --before-install SCRIPTS/before-install.sh \
        --after-install SCRIPTS/after-install.sh \
        --before-remove SCRIPTS/before-remove.sh \
        --after-remove SCRIPTS/after-remove.sh \
        -C buildroot .


}


upload(){
# Upload to repository
echo "Upload RPM to yum registry"

curl -v -u $NEXUS_USER:$NEXUS_PASS --upload-file molgenis-emx2-$VERSION-$RELEASE.noarch.rpm     https://registry.molgenis.org/repository/molgenis-emx2-yum-unstable/
echo "Upload DEB to apt noble  registry"
curl -v -u $NEXUS_USER:$NEXUS_PASS -H Content-Type: multipart/form-data --data-binary @./molgenis-emx2_$VERSION-${RELEASE}_all.deb https://registry.molgenis.org/repository/molgenis-emx2-noble-unstable/
echo "Upload RPM to apt jammy registry"
curl -v -u $NEXUS_USER:$NEXUS_PASS -H Content-Type: multipart/form-data --data-binary @./molgenis-emx2_$VERSION-${RELEASE}_all.deb https://registry.molgenis.org/repository/molgenis-emx2-jammy-unstable/


}

check_req
create_buildroot
create_builds
upload






