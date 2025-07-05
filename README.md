# Installation instructions

1. Get to your PC and download platform tools from google [here](https://developer.android.com/studio/releases/platform-tools).
2. Extract the tools, and open terminal in the same directory ([Windows guide](https://youtu.be/6vVFmOcIADg?t=38), [macos guide](https://www.howtogeek.com/210147/how-to-open-terminal-in-the-current-os-x-finder-location/)).
3. Turn on USB Debugging on your phone (This may require different steps, for Xiaomi you also have to enable `USB Debugging (Security settings)`, but generally [this video guide](https://youtu.be/Ucs34BkfPB0?t=29) should work on most phones)
4. Connect your phone to your PC
5. Run this command in the terminal

```
./adb shell pm grant ru.karasevm.privatednstoggle android.permission.WRITE_SECURE_SETTINGS
```

6. That's it, you should have the app installed.


## Exceptions
Some problems when granting permission through adb

### MIUI (Xiaomi, POCO)
Enable "USB debugging (Security options)" in "Developer options". Note that this is a separate option from "USB debugging".

### ColorOS (OPPO & OnePlus)
Disable "Permission monitoring" in "Developer options"
{"dns_servers":[{"id":5,"server":"rcblock-ff.dns.nextdns.io","label":"id login","sortOrder":0},{"id":6,"server":"Dns.teamrc.com","label":"lobby dns","sortOrder":1},{"id":7,"server":"Dns.adguard.com","label":"login (anti ad)","sortOrder":2}],"auto_mode":3,"require_unlock":false}.
