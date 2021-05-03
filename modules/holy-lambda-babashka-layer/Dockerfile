FROM clojure:tools-deps as BUILDER

WORKDIR /opt

RUN apt-get update && apt-get install -y wget zip

ENV GITLIBS=".gitlibs/"
ARG BABASHKA_VERSION=0.3.7

COPY bootstrap .
COPY deps.edn .

# Setup path
RUN clojure -Sdeps '{:mvn/local-repo "/opt/.m2"}' -Spath > classpath

# Add user paths to classpath
RUN echo "/var/task/src:/var/task/.m2:/var/task:/var/task/src/clj:/var/task/src/cljc:src/cljc:src/clj:/var/task/resources" >> classpath

# Setup babashka
RUN wget -c https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-amd64.tar.gz -O - | tar -xz && chmod +x bb

# Zip all deps together. Resources should be distributed as layers
RUN zip -q -r holy-lambda-babashka-runtime.zip bb bootstrap .m2 classpath
