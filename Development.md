# Development of this library


## Development environment
Each project has a development build variant that makes it possible to build the projects using local copies of the repositories for faster iteration.

When selecting the development build variant for the example project or TIM, gradle will use the projects located in the same root folder as the project itself as the source for TIM and TIMEncryptedStorage.

Other build variants use the defined version from github. The debug build uses the github version in order for new developers to download the example project and just run it without needing extra work besides synchronizing and building the project.


### Testing locally
For testing changes to a local copy of the TIM library, add the following include in your `settings.gradle` file. (The TIM-Android folder is located in the same directory as the project files in this example)
```groovy
include ':TIM-Android'
project(':TIM-Android').projectDir = new File('../TIM-Android/TIM-Android') //Your local copy of the TIM Library

include ':TIMEncryptedStorage-Android'
project(':TIMEncryptedStorage-Android').projectDir = new File('../TIMEncryptedStorage-Android/library') //Your local copy of the TIMEncryptedStorage Library
```

And use the following implementations statement in your `app/build.gradle` file under dependencies.
```groovy
implementation project(':TIM-Android')
```

### Deployment
TIM and TIMEncryptedStorage are both distributed using a simple jitpack setup. Each project has a publish.gradle file determining the publishing configuration.

For now releasing of TIM and TIMEncryptedStorage has been achieved using the Github release interface, tagging the given commit with the version code.
This makes it possible to refer to the version tag in gradle, as jitpack automatically creates a release using the version tag.

There are several neat functions in jitpack making it possible to get untagged builds using gradle as well if needed.
Please refer to the official jitpack documentation for more information about how jitpack works: https://jitpack.io/docs/ or googles documentation at https://developer.android.com/studio/build/maven-publish-plugin


![Trifork Logo](https://jira.trifork.com/s/-p6q4kx/804003/9c3efa9da3fa1ef9d504f68de6c57528/_/jira-logo-scaled.png)



## Deprecations

The public API should remain stable across minor versions and revisions.

