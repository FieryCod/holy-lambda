FROM ghcr.io/graalvm/graalvm-ce:ol8-java8-21.0.0.2
MAINTAINER Karol Wójcik <Karol Wójcik>

ARG RESULT_LIB="/staticlibs"
ARG BABASHKA_VERSION=0.3.5

RUN wget -c https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-amd64.tar.gz -O - | tar -xz
RUN chmod +x bb
RUN mv bb /bin/bb

RUN gu install native-image

# BEGIN PRE-REQUISITES FOR STATIC NATIVE IMAGES FOR GRAAL 21.0.0.2
# Based on https://github.com/oracle/graal/issues/2824
# SEE: https://github.com/oracle/graal/blob/master/substratevm/StaticImages.md
#
# ------------------------------------
# Credits to @emrul
# ------------------------------------
RUN mkdir ${RESULT_LIB} && \
  curl -L -o musl.tar.gz https://musl.libc.org/releases/musl-1.2.1.tar.gz && \
  mkdir musl && tar -xvzf musl.tar.gz -C musl --strip-components 1 && cd musl && \
  ./configure --disable-shared --prefix=${RESULT_LIB} && \
  make && make install && \
  cd / && rm -rf /muscl && rm -f /musl.tar.gz

RUN cp /usr/lib/gcc/x86_64-redhat-linux/8/libstdc++.a ${RESULT_LIB}/lib/

ENV PATH="$PATH:${RESULT_LIB}/bin"
ENV CC="musl-gcc"

RUN curl -L -o zlib.tar.gz https://zlib.net/zlib-1.2.11.tar.gz && \
  mkdir zlib && tar -xvzf zlib.tar.gz -C zlib --strip-components 1 && cd zlib && \
  ./configure --static --prefix=${RESULT_LIB} && \
  make && make install && \
  cd / && rm -rf /zlib && rm -f /zlib.tar.gz

# ------------------------------------
# END PRE-REQUISITES FOR STATIC NATIVE IMAGES FOR GRAAL 21.0.0.2
