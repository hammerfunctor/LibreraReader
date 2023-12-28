#!/bin/bash


export LIBRERA_TAG=8.9.133
export MUPDF_VERSION=1.23.7

export NDK_HOME=$HOME/AndroidSDK/ndk


set -e

if [[ -d $NDK_HOME ]]; then
  export NDK_PATH=$NDK_HOME/$(ls $NDK_HOME | sort -r | head -n 1)
else
  echo "Setup NDK_HOME first!!!"
  exit 1
fi
#git checkout $LIBRERA_TAG

sed -i -e '/^if.*Fdroid/,/^}/d' -e '/enable true/d' -e '/_appGdriveKey/d' -e '/_admob/d' app/build.gradle
mkdir app/src/main/jniLibs


cd Builder
rm -rf src/libs
#sed -i -e '/^#!/a\set -e' -e '/git clone/d' -e 's/mkdir/mkdir -p/g' link_to_mupdf_$MUPDF_VERSION.sh
sed -i -e '/^#!/a\set -e' -e 's/mkdir/mkdir -p/g' -e "s|\".*ndk-build\"|\"$NDK_PATH/ndk-build\"|" \
    -e "s|^.*ndk-build$|$NDK_PATH/ndk-build -j`nproc`|" link_to_mupdf_$MUPDF_VERSION.sh

#rm -rf mupdf-$MUPDF_VERSION/thirdparty/{harfbuzz/test,curl/tests,leptonica/prog/fuzzing}
./link_to_mupdf_$MUPDF_VERSION.sh

cd ..
if [[ -f $HOME/.gradle/gradle.properties ]]; then
  ./gradlew assembleFdroid
else
  echo "Setup key first!!!"
  exit 1
fi
