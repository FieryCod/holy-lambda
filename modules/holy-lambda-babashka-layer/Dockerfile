FROM clojure:tools-deps as BUILDER

WORKDIR /opt

RUN apt-get update && apt-get install -y wget zip

ENV GITLIBS=".gitlibs/"
ENV CLOJURE_TOOLS_DIR=/opt
ARG BABASHKA_VERSION=0.4.6

COPY bootstrap .
COPY deps.edn .
COPY hacks.clj .

RUN rm -Rf /opt/.m2

# Setup path
RUN clojure -Sdeps '{:mvn/local-repo "/opt/.m2"}' -Spath

# Setup babashka
RUN wget -c https://github.com/babashka/babashka/releases/download/v$BABASHKA_VERSION/babashka-$BABASHKA_VERSION-linux-amd64.tar.gz -O - | tar -xz && chmod +x bb

# # Setup deps.tools
# RUN curl https://raw.githubusercontent.com/borkdude/deps.clj/master/install | bash && deps -P

# Zip all deps together. Resources should be distributed as layers
RUN zip -q -r holy-lambda-babashka-runtime.zip bb bootstrap .m2 hacks.clj
