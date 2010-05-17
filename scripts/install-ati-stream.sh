#!/bin/bash

#
# Here we have unpacked ati-stream-sdk-v2.1-lnx64.tgz from http://developer.amd.com/gpu/ATIStreamSDK/
# to /opt-linux-x86_64/ and made a symbolic link to ati-stream-sdk.
#
# We also copied the file http://developer.amd.com/Downloads/icd-registration.tgz
# into ati-stream-sdk.

SDK=/opt-linux-x86_64/ati-stream-sdk/
ICDREG=/opt-linux-x86_64/ati-stream-sdk/icd-registration.tgz

link(){
    if [ -e $2 ] ; then
        rm -v $2
    fi
    ln -v -s ${SDK}$1/$2
}

cd /usr/lib64/
link lib/x86_64 libOpenCL.so
link lib/x86_64 libatiocl64.so

cd /usr/lib32/
link lib/x86 libOpenCL.so
link lib/x86 libatiocl32.so

cd /
tar xzf $ICDREG
