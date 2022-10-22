FROM clojure:tools-deps as BUILDER

WORKDIR /opt

RUN apt-get update && apt-get install -y wget zip

ENV GITLIBS=".gitlibs/"
ENV CLOJURE_TOOLS_DIR=/opt
ARG BABASHKA_VERSION=0.9.162

COPY bootstrap .
COPY hacks.clj .

# Setup babashka
RUN wget -c https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-aarch64.tar.gz -O - | tar -xz && chmod +x bb

# Zip all deps together. Resources should be distributed as layers
RUN zip -q -r holy-lambda-babashka-runtime-arm64.zip bb bootstrap hacks.clj
