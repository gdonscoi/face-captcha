# Certiface FRAMEWORK v0.1 (Android)

## Download

Download [the latest AAR](#)

Certiface requires at minimum Java 7 and Android 4.0 (API 14)

--

## Import the .AAR library in Android Studio


1. Open your project in Android Studio
1. Download the library
1. Go to File > New > New Module
1. Select "Import .JAR/.AAR Package" and click next
1. Enter the path to .aar file and click finish
1. Go to File > Project Structure > Dependencies Tab
1. Under "Modules," in left menu, select the target module app
1. Go to "Dependencies" tab
1. Click in the '+' button
1. Select module dependency
1. Select the created module from the list

--

## API usage

- Include the folow dependecies in your `build.gradle`:
```
implementation "com.squareup.retrofit2:retrofit:2.3.0"
implementation "com.squareup.retrofit2:converter-gson:2.3.0"
implementation "org.bouncycastle:bcprov-jdk16:1.45"
```

- Start the activity `br.com.oiti.certiface.challenge.ChallengeActivity.java` passing the three parameters below:
  - `ChallengeActivity.PARAM_ENDPOINT`: Server endpoint
  - `ChallengeActivity.PARAM_APP_KEY`:  User application key
  - `ChallengeActivity.PARAM_USER_INFO`: User sensible data

- The activity will return:
  - `Activity.RESULT_CANCELED`: When user cancel or an error occurred
    - In this case, the activity will return extra parameters:
      - `ChallengeActivity.PARAM_RESULT_ERROR`: Message of error
  - `Activity.RESULT_OK`: When API validate with success
    - In this case, the activity will return with two extra parameters:
      - `ChallengeActivity.PARAM_RESULT_HASH`: Hash used in server validation proccess
      - `ChallengeActivity.PARAM_RESULT_PROTOCOL`: Protocol generated in server validation proccess 


For more details, take a look at the `sample` dir.


## Dependecies

Certiface uses retrofit, then if you're using ProGuard you might need to add the following options:

```
-dontwarn okio.**
-dontwarn javax.annotation.**
```
