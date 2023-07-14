FROM gitpod/workspace-full:latest

RUN bash -c ". .nvm/nvm.sh \
    && nvm install 14 \
    && nvm alias default 14"

RUN echo "sdk install java 17.0.0-open" | bash
