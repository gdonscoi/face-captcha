# Certiface FRAMEWORK v0.1 (Android)

## Download

Download [the latest JAR]

Certiface requires at minimum Java 7 and Android 4.0 (API 14)

--

## Import the library in Android Studio

`File > Project Structure > Dependencies Tab > Add module dependency (scope = compile)`

1. Open your project in Android Studio
2. Download the library
3. Go to File > Project Structure > Dependencies Tab
4. Click in the '+' button > jar dependency (scope = compile)
4. Select the downloaded library

--

## API usage

Start the activity `br.com.oiti.certiface.challenge.ChallengeActivity.java` passing the three parameters below:
- `ChallengeActivity.PARAM_ENDPOINT`: Server endpoint
- `ChallengeActivity.PARAM_APP_KEY`:  User application key
- `ChallengeActivity.PARAM_USER_INFO`: User sensible data

The activity will result:
- `Activity.RESULT_CANCELED`: When user cancel
- `Activity.RESULT_OK`: When API validate with success
  - In this case, the activity will return with two extra parameters:
    - `ChallengeActivity.RESULT_HASH`: Hash used in server validation proccess
    - `ChallengeActivity.RESULT_PROTOCOL`: Protocol generated in server validation proccess 

--

## Dependecies
Certiface uses retrofit, then if you're using ProGuard you might need to add the following options:

```
-dontwarn okio.**
-dontwarn javax.annotation.**
```
