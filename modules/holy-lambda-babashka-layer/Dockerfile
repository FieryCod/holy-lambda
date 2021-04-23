FROM clojure:tools-deps as BUILDER

WORKDIR /var/task

RUN apt-get update && apt-get install -y wget zip

ENV GITLIBS=".gitlibs/"
ENV _JAVA_OPTIONS=-Duser.home=.
ENV XDG_CACHE_HOME=.
ARG BABASHKA_VERSION=0.3.5

COPY bootstrap .
COPY deps.edn .
COPY download_base_pods.clj .

# Setup path
RUN clojure -Sdeps '{:mvn/local-repo ".m2/repository"}' -Spath > classpath

RUN wget -c https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-amd64.tar.gz -O - | tar -xz

RUN chmod +x bb
RUN ./bb download_base_pods.clj

RUN zip -q -r holy-lambda-babashka-runtime.zip bb bootstrap .m2 .babashka
