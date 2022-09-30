#### Local dev
Start `WmApplication.java` with `local` profile.  
Do not forget to add the following parameter to be able to use profiles (local)

```
-Dspring.config.additional-location=file:c:\dev_home\workspaceGIT\iris-sr-jdk-17\iris-sr-wm\iris-sr-wm-install\src\main\resources\home\config\ 
-Djavax.net.ssl.trustStore=C:/dev_home/workspaceGIT/iris-sr-jdk-17/iris-sr-wm/iris-sr-wm-app/src/test/resources/cas.jks
```

#### Hosts

### Exposed interfaces
The list of interfaces is available here : http://hostname:port/swagger-ui.html

There are two groups of interfaces :
- `private`: they are exposed for the internal iris components (Ex: CMS -> STI).
- `public`: they are exposed for the external components (PDT, DiLi, ...) (Ex: PDT -> CMS)

To add a new private interface use @PrivateApi annotation, for a public interface use @PublicApi annotation.

Example : 

```
@GetMapping
@PrivateApi
public ResponseEntity<File> downloadMeta( ... ) {
    ...
}
```


#### Profiles
Check [profiles](iris-sr-wm-install/src/main/resources/home/config/)

### Deploy RPM manually

#### If an rpm is already installed, check the app status, stop the app and uninstall the rpm
`$>systemctl status iris-wm` 

`$>systemctl stop iris-wm`

`$>dnf erase iris-sr-wm`

#### Install from redhat satellite repository
`$>dnf info iris-sr-wm`

--> Check available version, if the version is appropriate, install it

`$>dnf install iris-sr-wm`

If version available is not appropriate, clean repositories

`$>dnf clean all` --> update local data

--> Try again from `$>dnf info iris-sr-wm`

#### Install manually from local rpm (/!\only for experimental use, not recommended)
`$>dnf localinstall {path-to-rpm/or rpm url}`

#### Start and verify
`$>systemctl start iris-wm`

`$>systemctl status iris-wm`

