VERSION=$1
FILE=$2
RELEASE=1
NAME=molgenis-emx2
SUMMARY="The world's most customizable platform for (scientific) data and FAIR principles (findability, accessibility, interoperability and reusability)."
BUILD_ARCH=noarch
LICENSE=LGPL3
WEB=https://www.molgenis.org
USER=molgenis
GROUP=molgenis
REGISTRY_USER=$2
REGISTRY_PASS=$3

# First create the buildroot
mkdir -p buildroot/usr/local/share/molgenis
mkdir -p buildroot/var/log/molgenis/
mkdir -p buildroot/etc/systemd/system/

#Copy files to the buildroot
cp ci-build-packages/log4j2.xml buildroot/usr/local/share/molgenis/
cp ci-build-packages/molgenis-emx2.service buildroot/etc/systemd/system/
#CP JAR FILE TO BUILDROOT
cp ~/repo/build/libs/$FILE buildroot/usr/local/share/molgenis/ 
ln -s buildroot/usr/local/share/molgenis/$FILE buildroot/usr/local/share/molgenis/molgenis-emx2.jar





# RPM Creation
fpm -t rpm -s dir \
        -f -n "$NAME" -v "$VERSION" --iteration "$RELEASE" -a "$BUILD_ARCH" \
        --description "$SUMMARY" --url "$WEB_URL" --license "$LICENSE" --vendor "$VENDOR" \
        --depends "tar" \
        --before-install ci-build-packages/SCRIPTS/before-install.sh \
        --after-install ci-build-packages/SCRIPTS/after-install.sh \
        --before-remove ci-build-packages/SCRIPTS/before-remove.sh \
        --after-remove ci-build-packages/SCRIPTS/after-remove.sh \
        --verbose -C buildroot .
#DEB Creation
fpm -t deb -s dir \
        -f -n "$NAME" -v "$VERSION" --iteration "$RELEASE" -a "$BUILD_ARCH" \
        --description "$SUMMARY" --url "$WEB_URL" --license "$LICENSE" --vendor "$VENDOR" \
        --depends "tar" \
        --before-install ci-build-packages/SCRIPTS/before-install.sh \
        --after-install ci-build-packages/SCRIPTS/after-install.sh \
        --before-remove ci-build-packages/SCRIPTS/before-remove.sh \
        --after-remove ci-build-packages/SCRIPTS/after-remove.sh \
        --verbose -C buildroot .


# Upload to repository

curl -v -u $NEXUS_USER:$NEXUS_PASS --upload-file molgenis-emx2-$VERSION-$RELEASE.noarch.rpm     https://registry.molgenis.org/repository/molgenis-emx2/10/unstable/
curl -v -u $NEXUS_USER:$NEXUS_PASS -H Content-Type: multipart/form-data --data-binary @./molgenis-emx2_$VERSION-$RELEASE_all.deb https://registry.molgenis.org/repository/molgenis-emx2-jammy-unstable/

