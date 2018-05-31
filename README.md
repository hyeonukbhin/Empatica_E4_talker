# Empatica E4 data reciever
Empatica E4 data reciever
ROS:android_core의 android_tutorial_pubsub project 기반으로 동작합니다.

## Requirements

### Android Studio & Tools Download
Downloading the build tools, sdk and android studio.
Setting-Appearance & Behavior - Android SDK - SDK Platforms
* API level 19~25 install

Setting-Appearance & Behavior - Android SDK - SDK Tools
* Android Emulator
* Google Web Driver

Setting - Build, Execution, Deployment - Instant Run
* Check Disable


### Required Package Installation
``
sudo apt-get install ros-kinetic-catkin ros-kinetic-rospack python-wstool openjdk-8-jdk
``
### ~/.bahsrc setting
자신의 Android Studio & SDK Tools 환경에 따라서 다르게 설정해주세요.
``
echo export PATH=\${PATH}:/opt/android-sdk/tools:/opt/android-sdk/platform-tools:/opt/android-studio/bin >> ~/.bashrc
echo export ANDROID_HOME=/opt/android-sdk >> ~/.bashrc
``
### rosjava installation
``
mkdir -p ~/rosjava/src
wstool init -j4 ~/rosjava/src https://raw.githubusercontent.com/rosjava/rosjava/kinetic/rosjava.rosinstall
source /opt/ros/kinetic/setup.bash
cd ~/rosjava
rosdep update
rosdep install --from-paths src -i -y
catkin_make
``

### android_core install
- [android_core : Github](https://github.com/rosjava/android_core)
- [Ref. tutorials](http://wiki.ros.org/android_core/Tutorials)


### empatica_e4_msgs Build
- [empatica_e4_msgs : Github](https://github.com/hyeonukbhin/empatica_e4_msgs)

## App Build
1. android_core 를 Android Studio에서 Project로 OPEN
2. android_core/android_tutorial_pubsub(origin) Build
3. Overwiting Folder : android_tutorial_pubsub
4. android_core/android_tutorial_pubsub(modified, Empatica E4 Talker) Build

## Usage
1. App(Empatica E4 Talker) 실행
2. Empatica E4 Pairing
3. rostopic echo /physiology_data

## Reference
* EMPATICA_API_KEY = "290b36eae902472c891ea98e7bb2368d"
