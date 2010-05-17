#! /bin/sh

if [ -e ../setenv-build-jogl-x86_64.sh ] ; then
    . ../setenv-build-jogl-x86_64.sh
fi

ant  \
    -Drootrel.build=build-x86_64 \
    $* 2>&1 | tee make.jocl.all.linux-x86_64.log
