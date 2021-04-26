FROM ghcr.io/graalvm/graalvm-ce:ol8-java8-21.1.0
MAINTAINER Karol Wójcik <Karol Wójcik>

RUN microdnf install unzip curl make wget

ARG BABASHKA_VERSION=0.3.6
ARG CLOJURE_VERSION=1.10.3.822

RUN wget -c https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-amd64.tar.gz -O - | tar -xz
RUN chmod +x bb
RUN mv bb /bin/bb

RUN mkdir -p /project/.holy-lambda/

COPY download_pods .
RUN chmod +x download_pods
RUN mv download_pods /bin/download_pods

ENV _JAVA_OPTIONS=-Duser.home=/
ENV XDG_CACHE_HOME=/
ENV XDG_CONFIG_HOME=/
ENV CLJ_CACHE=/
ENV CLOJURE_TOOLS_DIR=/
ENV HOME=/

# Install deps
RUN curl https://raw.githubusercontent.com/borkdude/deps.clj/master/install | bash
RUN deps -P

# Install clojure
RUN curl https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh | bash

# Install aws command
RUN curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" \
    && unzip awscliv2.zip -d aws \
    && ./aws/aws/install \
    && rm -Rf awscliv2.zip aws

# Install aws-sam command
RUN curl -L "https://github.com/aws/aws-sam-cli/releases/latest/download/aws-sam-cli-linux-x86_64.zip" -o "aws-sam.zip" \
 && unzip aws-sam.zip -d aws-sam \
 && ./aws-sam/install \
 && rm -Rf aws-sam aws-sam.zip

# Install leiningen
RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o lein \
    && chmod +x lein \
    && mv lein /usr/bin/lein

RUN lein --version

ARG RESULT_LIB="/staticlibs"

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

RUN mkdir -p /.babashka
RUN chmod -R 777 /clojure
RUN chmod -R 777 /.m2
RUN chmod -R 777 /.babashka

WORKDIR /project

ENV _JAVA_OPTIONS=-Duser.home=/project/.holy-lambda
ENV XDG_CACHE_HOME=/project/.holy-lambda
ENV XDG_CONFIG_HOME=/project/.holy-lambda
