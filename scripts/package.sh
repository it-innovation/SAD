#!/bin/sh

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
ROOT="$SCRIPTPATH"/..
cd "$ROOT"

# install pre-requisites for packaging
apt-get install -y make
apt-get install -y python-setuptools
easy_install -U Sphinx
apt-get install -y zip

# build the docs
cd "$ROOT"/docs
make clean html |tee /tmp/releasedoc.log

# install the third-party libs (later removed from distribution)
cd "$ROOT"/lib
./install_into_maven.sh

# build
cd "$ROOT"
mvn clean
mvn -Ddependency.locations.enabled=false package assembly:assembly -Preleasesrc |tee /tmp/releasesrc.log |grep -E 'WARNING|ERROR'
mvn -Ddependency.locations.enabled=false package assembly:assembly -Preleasebin |tee /tmp/releasebin.log |grep -E 'WARNING|ERROR'

echo
echo "Check /tmp/release*.log for further information."
