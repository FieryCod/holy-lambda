FROM arm64v8/amazonlinux
MAINTAINER Karol Wójcik <Karol Wójcik>

RUN yum install unzip curl make wget git zip

ENV CLJ_CACHE=/project/.holy-lambda/clojure
ENV CLJ_COFIG=/project/.holy-lambda/clojure
ENV CLOJURE_TOOLS_DIR=/project/.holy-lambda
ENV HOME=/project/.holy-lambda
ENV XDG_CACHE_HOME=/project/.holy-lambda
ENV XDG_CONFIG_HOME=/project/.holy-lambda
ENV XDG_DATA_HOME=/project/.holy-lambda
ENV GITLIBS=/project/.holy-lambda/.gitlibs/

# --------------- UPDATE ------------------
# -----------------------------------------
ARG BABASHKA_VERSION=0.4.6
ARG CLOJURE_VERSION=1.10.3.849
# -----------------------------------------

ARG JAVA_VERSION=java11
ARG GRAALVM_VERSION_POSTFIX=20210706_1943
ARG GRAALVM_VERSION=21.3.0-dev

ENV LANG=en_US.UTF-8 \
    JAVA_HOME=/opt/graalvm-ce-$JAVA_VERSION-$GRAALVM_VERSION

# Install gu
RUN curl https://raw.githubusercontent.com/graalvm/container/master/community/gu-wrapper.sh > gu-wrapper.sh
RUN mv gu-wrapper.sh /usr/local/bin/gu

ARG GRAALVM_JAVA_URL=https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/$GRAALVM_VERSION-$GRAALVM_VERSION_POSTFIX/graalvm-ce-java11-linux-aarch64-dev.tar.gz
ARG GRAALVM_NATIVE_IMAGE_URL=https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/$GRAALVM_VERSION-$GRAALVM_VERSION_POSTFIX/native-image-installable-svm-java11-linux-aarch64-dev.jar

ARG GRAALVM_JAVA_CORD=graalvm-ce-linux.tar.gz
ARG GRAALVM_NATIVE_IMAGE_CORD=native-image.jar

RUN curl -L $GRAALVM_JAVA_URL > $GRAALVM_JAVA_CORD
RUN curl -L $GRAALVM_NATIVE_IMAGE_URL > $GRAALVM_NATIVE_IMAGE_CORD

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

# Provide download_pods command
COPY download_pods download_pods.clj /
RUN chmod +x download_pods && mv download_pods /bin/download_pods

# Install native-image
RUN gu -L install $GRAALVM_NATIVE_IMAGE_CORD

# Cleanup
RUN rm -Rf $GRAALVM_JAVA_CORD $GRAALVM_NATIVE_IMAGE_CORD

# Install babashka
RUN wget -c https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-aarch64.tar.gz -O - | tar -xz \
    && chmod +x bb \
    && mv bb /bin/bb

# Install clojure
RUN curl https://download.clojure.org/install/linux-install-$CLOJURE_VERSION.sh | bash

RUN alias java="`which java` -Duser.home=/project/.holy-lambda"
RUN alias bb="`which bb` -Duser.home=/project/.holy-lambda"
RUN alias clojure="`which clojure` -J-Duser.home=\"/project/.holy-lambda\""
RUN alias deps="`which deps` -J-Duser.home=/project/.holy-lambda"

RUN gu install native-image

WORKDIR /project