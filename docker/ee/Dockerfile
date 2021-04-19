FROM oraclelinux:8-slim
MAINTAINER Karol Wójcik <Karol Wójcik>

# Based on https://github.com/graalvm/container/blob/master/community/Dockerfile.ol8
RUN microdnf update -y oraclelinux-release-el8 \
    && microdnf --enablerepo ol8_codeready_builder install bzip2-devel ed gcc gcc-c++ gcc-gfortran gzip file fontconfig less libcurl-devel make openssl openssl-devel readline-devel tar \
    vi which xz-devel zlib-devel findutils glibc-static libstdc++ libstdc++-devel libstdc++-static zlib-static \
    && microdnf clean all
RUN fc-cache -f -v

ARG GRAALVM_VERSION=21.0.0.2
ARG GRAALVM_JAVA_CORD=graalvm-ee-java8-linux-amd64-$GRAALVM_VERSION.tar.gz
ARG GRAALVM_NATIVE_IMAGE_CORD=native-image-installable-svm-svmee-java8-linux-amd64-$GRAALVM_VERSION.jar

ARG JAVA_VERSION=java8

ENV LANG=en_US.UTF-8 \
    JAVA_HOME=/opt/graalvm-ee-$JAVA_VERSION-$GRAALVM_VERSION

# Install gu
RUN curl https://raw.githubusercontent.com/graalvm/container/master/community/gu-wrapper.sh > gu-wrapper.sh

RUN mv gu-wrapper.sh /usr/local/bin/gu

COPY $GRAALVM_JAVA_CORD .
COPY $GRAALVM_NATIVE_IMAGE_CORD .

# Install EE Java edition
RUN set -eux && gunzip -dc $GRAALVM_JAVA_CORD | tar x -C /opt/ \
    # Set alternative links
    && mkdir -p "/usr/java" \
    && ln -sfT "$JAVA_HOME" /usr/java/default \
    && ln -sfT "$JAVA_HOME" /usr/java/latest \
    && for bin in "$JAVA_HOME/bin/"*; do \
    base="$(basename "$bin")"; \
    [ ! -e "/usr/bin/$base" ]; \
    alternatives --install "/usr/bin/$base" "$base" "$bin" 20000; \
    done \
    && chmod +x /usr/local/bin/gu

# Install native-image
RUN gu -L install $GRAALVM_NATIVE_IMAGE_CORD

# Cleanup
RUN rm -Rf $GRAALVM_JAVA_CORD $GRAALVM_NATIVE_IMAGE_CORD

ARG MUSL_RESULT_LIB="/staticlibs"

# BEGIN PRE-REQUISITES FOR STATIC NATIVE IMAGES FOR GRAAL 21.0.0.2
# Based on https://github.com/oracle/graal/issues/2824
# SEE: https://github.com/oracle/graal/blob/master/substratevm/StaticImages.md
#
# ------------------------------------
# Credits to @emrul
# ------------------------------------
RUN mkdir ${MUSL_RESULT_LIB} && \
  curl -L -o musl.tar.gz https://musl.libc.org/releases/musl-1.2.1.tar.gz && \
  mkdir musl && tar -xvzf musl.tar.gz -C musl --strip-components 1 && cd musl && \
  ./configure --disable-shared --prefix=${MUSL_RESULT_LIB} && \
  make && make install && \
  cd / && rm -rf /muscl && rm -f /musl.tar.gz

RUN cp /usr/lib/gcc/x86_64-redhat-linux/8/libstdc++.a ${MUSL_RESULT_LIB}/lib/

ENV PATH="$PATH:${MUSL_RESULT_LIB}/bin"
ENV CC="musl-gcc"

RUN curl -L -o zlib.tar.gz https://zlib.net/zlib-1.2.11.tar.gz && \
  mkdir zlib && tar -xvzf zlib.tar.gz -C zlib --strip-components 1 && cd zlib && \
  ./configure --static --prefix=${MUSL_RESULT_LIB} && \
  make && make install && \
  cd / && rm -rf /zlib && rm -f /zlib.tar.gz

# ------------------------------------
# END PRE-REQUISITES FOR STATIC NATIVE IMAGES FOR GRAAL 21.0.0.2

CMD java -version
