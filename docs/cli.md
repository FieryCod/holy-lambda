# Document Title

❯ bb tasks
The following tasks are available:

hl:docker:run             > Run command docker context 

----------------------------------------------------------------

hl:native:conf            > Provides native configurations for the application
hl:native:executable      > Provides native executable of the application

----------------------------------------------------------------

hl:sync                   > Syncs project & dependencies from either:
                            - <Clojure>  deps.edn
                            - <Babashka> bb.edn:runtime:pods
hl:compile                > Compiles sources if necessary
                            - :force - force compilation even if sources did not change
hl:doctor                 > Diagnoses common issues of holy-lambda project
hl:clean                  > Cleanes build artifacts
hl:version                > Outputs holy-lambda babashka tasks version
