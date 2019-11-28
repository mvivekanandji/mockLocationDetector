# Mock Location Detector
An android library to help detect mock locations

[![Generic badge](https://img.shields.io/badge/stage-development-informational.svg)](https://github.com/bad-mash/tord-android/)  
[![GitHub release](https://img.shields.io/github/release/Naereen/StrapDown.js.svg)](https://github.com/bad-mash/tord-android/releases/)  

![Twitter Follow](https://img.shields.io/twitter/follow/mvivekanandji?label=Follow%20me&style=social)

## Overview
Use this library to detect if location recieved from the device is mock location or not. This library can also be used to detect if apps that can soopf/mock location are installed on the device or not. Also this library helps to remove the mock or text locations providers so the actual location can be obtained.

To detect if apps that can soopf/mock location are installed two diffent (or combination of both can be used):
1. Search for apps that require *ACCESS_MOCK_LOCATION* permission by using **checkForAllowMockLocationsApps()** (API 17-)
2. Search for know apps using **checkForKnownMockApps()** 

## How mock loaction work

In Android, there is only one way to spoof device’s GPS (without rooting), and that’s to use Android’s built in Mock Location API located in Developer Options. In Android 6.0 and above specific app needs to be selected. In older versions, it’s just a simple check box that enables mock locations mode for any app on your device.

There are 5 variables that the mock location API asks for to mock your location: **latitude, longitude, altitude, speed, and accuracy.** Typically, most apps just change the latitude and longitude values to change your GPS location to some place in the world. But what about the other 3 values (altitude, speed, and accuracy)? Surprisingly, almost all GPS Spoofing apps set these values to *some constant number*, whether it’s 0, 1, or *some random number*, it’s a value that remains the same. Some apps do give the user the option to set these values via settings, but even then, the number never changes when the user is actively using the app and changing their location. 

## Download
-------
### Gradle:

Step 1. Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```

Step 2. Add the dependency
```groovy
dependencies {
	    implementation 'com.github.bad-mash:mockLocationDetector:-SNAPSHOT'
}
```

### Maven:

Step 1. Add it in your root build.gradle at the end of repositories:

```groovy
<repositories>
	...
	<repository>
		<id>jitpack.io</id>
		 <url>https://jitpack.io</url>
	</repository>
</repositories>
```

Step 2. Add the dependency
```groovy
<dependency>
	...
	<groupId>com.github.bad-mash</groupId>
	<artifactId>mockLocationDetector</artifactId>
	<version>Tag</version>
</dependency>
```

## Usage

From anywhere on your code just call the class MockLocationDetector to access the following available static methods

1. `isAllowMockLocationsEnabled` *[Depricated]*
2. `isMockLocation` 
3. `isMockLocationOrMockEnabled` *[Depricated]*
4. `checkForAllowMockLocationsApps`
5. `checkForKnownMockApps`
6. `removeMockLocationProvider`

To detect if Mock location setting is enabled onthe device, just call the method `isAllowMockLocationsEnabled` and pass the context object. This method only works on Lollipop and below (API 22-). If this method is called on higher apis it then it will throw UnsupportedOperationException. This method id mainly for legacy use.

To detect if the location object you received is a mock, call the method `isMockLocation` and pass the location object. This method only works on Jelly Bean and above (API 18+). If this method is called on lower apis it then it will throw UnsupportedOperationException. This should be preferred and is recommended method for most cases.

To detect if the location object you received is a mock, call the method `isLocationFromMockProvider` and pass the context and the location object. This method only works on Jelly Bean and above (API 18+). If this method is called on lower apis it then it will check if Allow Mock Locations is ON or not and return the result. This method (though universal in terms pf api) should be used only if necessary (*prefer above two implementations*).

To detect if there are apps on the device that have "ALLOW_MOCK_LOCATIONS" permission on their manifest, call `checkForAllowMockLocationsApps`. This method is usefull for apps that use root access to enable/disable allow mock locations at runtime on lower apis (API 17 or lower). Starting on Marshmallow its no longer possible to enable/disable allow mock locations at runtime with root permissions, user selects the app that will be used for mock locations.

To detect if any already know(popular) mock app is installed on the device, call `checkForKnownMockApps`  


## Contributors
* Developer - *[Vivekanand Mishra](https://github.com/bad-mash)*
* Tester - *[Vivekanand Mishra](https://github.com/bad-mash)*


## License
[Apache License]( http://www.apache.org/licenses/LICENSE-2.0)
**Copyright 2019 Vivekanand Mishra** <br>
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
