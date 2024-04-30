# PwnGPS
paw-gps but less hacky for pwnagotchi (and now a real app!).

# DISCALIMER
Early access software AND my first time making an Android app so BUGS WILL OCCUR, please report your findings.

# Installation
- Download latest apk from releases and install.
- Grant required permissions.
- Open up a web browser and confirm it's working by going to [localhost:42069/gps.xhtml](http://localhost:42069/gps.xhtml) (it might take a while to load, so please be patient).
- Add this to your `config.toml`. Replace ip address if necessary, this example uses the android bluetooth address.
```toml
main.plugins.paw-gps.enabled = true
main.plugins.paw-gps.ip = "192.168.44.1:42069"
```
- (Recommended) Exclude PwnGPS from power saving features. Varies per phone so use your favorite search engine to figure it out.
