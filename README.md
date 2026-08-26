# Essentials tools for Android nerds to work with the Android app.

## Features
- Upcoming calendar schedule of all calendar accounts together
- Schedule tile
- Android device info tile
- Combined Android and watch battery complication
- Your Android customizable controls
   - Remote lock
   - Sound mode toggle
   - Flashlight with brightness
   - Notification pulse controls
   - AOD controls
   - Tap to wake controls
- Are we there yet? info at a glance
- Sync sound mode with Android
- And more coming

## Screenshots

<p align="center">
  <img width="24%" alt="watch_media_2026-07-26_00_09_24" src="https://github.com/user-attachments/assets/ccc55e85-daad-4017-941e-c74da7a5d7fe" />
  <img width="24%" alt="watch_media_2026-07-26_00_09_33" src="https://github.com/user-attachments/assets/68a05eee-3633-4ec5-9878-b1268ee24b1d" />
  <img width="24%" alt="watch_media_2026-07-26_00_08_58" src="https://github.com/user-attachments/assets/73383d7c-8b68-46e8-99a4-6c8d0b1fc452" />
  <img width="24%" alt="watch_media_2026-07-26_00_09_43" src="https://github.com/user-attachments/assets/88b13df7-fd0e-4c18-b965-a73637b35aaa" />
</p>

<p align="center">
  <img width="24%" alt="watch_media_2026-07-26_00_09_57" src="https://github.com/user-attachments/assets/bacf1993-4edd-4f03-880a-acc24250820f" />
  <img width="24%" alt="watch_media_2026-07-26_00_08_47" src="https://github.com/user-attachments/assets/ed356684-201c-4b55-91c3-5ac7d8a01f9f" />
  <img width="24%" alt="watch_media_2026-07-26_00_08_40" src="https://github.com/user-attachments/assets/1ee13d5a-7cdc-4e56-a3c3-3b2b1d155912" />
  <img width="24%" alt="watch_media_2026-07-26_00_12_36" src="https://github.com/user-attachments/assets/4028d746-ba6c-4669-ac04-645d061a3a9b" />
</p>



https://github.com/user-attachments/assets/75cb7fa3-d988-49c3-b4d7-d6e889cdd386



## Requirements
- [Essentials](https://github.com/sameerasw/essentials) Android app installed on Phone


## Installation and Wireless ADB guide
You need to sideload the APK to the watch via ADB.
> It's a good idea to place the watch on the charger during this process to ensure it stays connected to WiFi which keeps wireless ADB active.
> <br>And also make sure your phone is not connected to the PC with ADB as both apps share the same package name, it could interfere.

### Prepare the watch
1. On your watch, visit Settings -> System -> About -> Versions
2. Tap on ```Build number``` 7 times, Developer options will be prompted
   <br><img width="200" alt="watch_media_2026-07-25_23_45_15" src="https://github.com/user-attachments/assets/1fafdded-ba29-454c-b77a-4c85d717fed5" />

3. Once enabled, navigate back to settings and ```Developer options``` will be the last entry, tap it
   <br><img width="200" alt="watch_media_2026-07-25_23_45_26" src="https://github.com/user-attachments/assets/64b7aa6d-b424-4419-9217-bee729915a5c" />

4. Enable ```ADB debugging``` & Enable ```Disable adb authorization timeout``` for easier access later
   <br><img width="200" alt="watch_media_2026-07-25_23_45_37" src="https://github.com/user-attachments/assets/fa5c8975-bd63-4ba4-be93-64aec7949176" />

5. Scroll down, go into ```Wireless debugging```
   <br><img width="200" alt="watch_media_2026-07-25_23_45_47" src="https://github.com/user-attachments/assets/27c6aa3a-17c2-4f85-a8a9-211c09ae6a24" />

6. Enable ```Wireless debugging```

### Pairing
7. On the PC, setup [platform tools](https://developer.android.com/tools/releases/platform-tools) and open a terminal in that directory
8. On the watch, select ```Pair new device```
    <br><img width="200" alt="watch_media_2026-07-25_23_46_08" src="https://github.com/user-attachments/assets/801a45ab-1886-41d4-9ea3-ef10cbf3dd2e" />

9. Enter the shown IP address and the port on the terminal
    <br><img width="200" alt="watch_media_2026-07-25_23_46_16" src="https://github.com/user-attachments/assets/079ade8b-fabf-4458-9216-5691b32794a2" />
     ```
     adb pair 192.168.1.163:33525
     ```
10. Enter the ```WiFi pairing code```, complete

11. Done

### Connection
12. Go back
13. Use the IP Address & port from the shown entry and run the below command to connect. This is different from the pairing IP and ports.
    <br><img width="200" alt="watch_media_2026-07-25_23_46_33" src="https://github.com/user-attachments/assets/e5ebf2ca-f5e1-43aa-837a-de7ef586a545" />

     ```
     adb connect 192.168.1.59:37359
     ```
     
### Installation
14. Once connected, run the below command to install the app.
    ```
    adb install app-release.apk
    ```

## Permissions
Grant the optional permissions via the adb shell for enabling all the features using the same adb connected terminal session.

- WRITE_SECURE_SETTINGS
  ```
  adb shell pm grant com.sameerasw.essentials android.permission.WRITE_SECURE_SETTINGS
  ```

- DND_ACCESS
  ```
  adb shell cmd notification allow_dnd com.sameerasw.essentials
  ```


---

<p align="center">
  <a href="https://www.reddit.com/r/MadebySameerasw"><img  width="49%"  alt=" reddit-banner" src="https://github.com/user-attachments/assets/a5197458-d64a-4c6a-a6a3-9e1f36030205" /></a>
  <a href="https://t.me/tidwib"><img  width="49%"  alt=" telegram-banner" src="https://github.com/user-attachments/assets/425b3cc1-9ac6-46ec-8f48-71c7af9c9ca2" /></a>
</p>

> <br>**Visit the website:** [sameerasw.com/essentials](https://sameerasw.com/essentials)
> <br>**About Essentials:** [README](https://github.com/sameerasw/essentials#navigation)
> <br>**More projects:** [sameerasw.com](https://sameerasw.com/#updates)
> <br>**Show some love:** [buymeacoffee](https://buymeacoffee.com/sameerasw) | [GitHub Sponsor](https://github.com/sponsors/sameerasw)
> 
> <img width="400" alt="madeby Medium" src="https://github.com/user-attachments/assets/cea162a1-4cbb-4b5b-b21b-80be522ab646" />


  
