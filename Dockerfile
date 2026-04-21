FROM ubuntu:22.04

# Java yükle
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    gradle \
    git \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Android SDK yükle
RUN mkdir -p /opt/android-sdk && \
    cd /opt/android-sdk && \
    curl -s https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -o cmdline-tools.zip && \
    unzip -q cmdline-tools.zip && \
    rm cmdline-tools.zip && \
    mkdir -p /opt/android-sdk/cmdline-tools/latest && \
    mv /opt/android-sdk/cmdline-tools/* /opt/android-sdk/cmdline-tools/latest/ 2>/dev/null || true

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

# SDK Components yükle (batch mode)
RUN yes | sdkmanager --sdk_root=$ANDROID_HOME \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "cmdline-tools;latest" || true

WORKDIR /app

# Build source code
COPY . .

# Build APK
RUN gradle build assembleDebug 2>&1 || echo "Build sonuçlandı (hatayla)"

CMD ["ls", "-la", "app/build/outputs/apk/debug/"]
