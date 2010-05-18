#! /bin/sh

if [ -e ../setenv-build-jogl-x86.sh ] ; then
    . ../setenv-build-jogl-x86.sh
fi

ant  \
    -Drootrel.build=build-x86 \
    $* 2>&1 | tee make.jocl.all.linux-x86.log
