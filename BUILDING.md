# Building SparkFactionAPI

## 中文

请从 SparkFactionAPI 的源码根目录构建。这个目录里应该能看到：

```text
build.gradle
settings.gradle
gradle.properties
gradlew
gradlew.bat
src/
libs/
```

Windows:

```bat
gradlew.bat clean build
```

macOS / Linux:

```sh
./gradlew clean build
```

项目可以放在任意父级路径下，源码根目录本身也可以改名；但不要从缺少这些文件的外层目录直接运行 `gradle build`。如果外层目录有自己的 `settings.gradle`，并且写死了 `include "SparkFactionAPI"`，那么外层必须真的存在一个名为 `SparkFactionAPI` 的子目录，否则 Gradle 会报 `projectDirectory ... does not exist`。

发布源码包时，推荐直接压缩源码根目录，或者保证外层包里的子目录名和外层 `settings.gradle` 完全一致。

## English

Build from the SparkFactionAPI source root. The project root should contain `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradlew.bat`, `src/`, and `libs/`.

Use the checked-in Gradle Wrapper:

```sh
./gradlew clean build
```

On Windows:

```bat
gradlew.bat clean build
```

The project may live under any parent path, and the source root folder may be renamed. Do not run `gradle build` from an outer folder unless that outer folder is itself a valid Gradle build. If an outer `settings.gradle` hard-codes `include "SparkFactionAPI"`, that exact child directory must exist.
