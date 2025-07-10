#!/bin/bash

# Configuration pour Android
export ANDROID_NDK_HOME=/path/to/android-ndk-r25b
export TOOLCHAIN=$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64
export TARGET=aarch64-linux-android
export API=21

# Variables d'environnement pour la compilation
export CC=$TOOLCHAIN/bin/$TARGET$API-clang
export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
export AR=$TOOLCHAIN/bin/llvm-ar
export STRIP=$TOOLCHAIN/bin/llvm-strip

# Configuration nmap avec chemins personnalisés
./configure \
    --host=$TARGET \
    --prefix=/data/local/tmp \
    --enable-static \
    --disable-shared \
    --disable-nls \
    --disable-ipv6 \
    --disable-zenmap \
    --disable-ncat \
    --disable-ndiff \
    --disable-nping \
    --disable-nmap-update \
    --disable-universal \
    --with-pcap=linux \
    --with-libpcap=included \
    --with-libpcre=included \
    --with-libdnet=included \
    --with-liblinear=included \
    --with-libssh2=included \
    --with-libz=included \
    --with-openssl=included

# Compilation
make -j$(nproc)

# Installation
make install DESTDIR=/tmp/nmap_android

echo "Nmap compilé avec succès !"
echo "Le binaire se trouve dans : /tmp/nmap_android/data/local/tmp/nmap"