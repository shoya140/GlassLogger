GlassLogger
===========

the sensor logging app on google glass

## Features

* record raw infra red sensor data -> /sdcard/GlassLogger/\_TIMESTAMP\_ir.txt

## Instration

### 1.Set up android sdk on Linux
 
fastboot on MacOSX has some trouble and doesn't work.<br>
Please download "adb" and "fastboot" commands and do path settiings on Linux.

### 2.Get the root permission of Google Glass

<pre>
$adb reboot bootloader
$fastboot oem unlock #You have to type this command twice.
$fastboot flash boot boot.img
$fastboot reboot
$adb root
</pre>

### 3.Change permissions of /sys/... raw sensor data files.

<pre>
#chmod 664 /sys/bus/i2c/devices/4-0035/proxraw
-rw-rw-r-- system   system       4096 2013-11-04 10:05 proxraw
</pre>

### 4.Build GlassLogger.apk file and install to GoogleGlass

Please build the .apk file by eclipse or android stdio.
