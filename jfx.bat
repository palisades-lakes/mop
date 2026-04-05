@echo off
:: palisades.lakes (at) gmail (dot) com
:: 2026-03-10

:: 'simple' launcher script for jfx main classes
::
:: want to be able to launch multiple java classes in project,
:: probably mostly in scripts/java/mop.java.something packages.
:: TODO: similar clojure launcher.
::
:: no modules in project --- module system seems like poorly thought thru
:: extra layer of complexity, for little reward.
::
:: using MAVEN to fetch dependencies to lib folder.
:: then call java binary directly.
::
:: an alternative is to use the javafx-maven-plugin, but that seems to require
:: a single main class.

::set XMX=-Xms56g -Xmx56g
::set XMX=-Xms31g -Xmx31g -Xmn12g
::set XMX=-Xms12g -Xmx12g -Xmn5g
set XMX=

set OPENS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/javax.imageio=ALL-UNNAMED --add-opens java.desktop/javax.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED

set CP=-cp lib\*

::set MODULE_PATH=--module-path lib\*
set JAVA="%JAVA_HOME%\bin\java"

set CMD=%JAVA% -ea -dsa  %XMX% %OPENS% %CP% --sun-misc-unsafe-memory-access=allow %MODULE_PATH% %MODULES%  --enable-native-access=javafx.graphics %*

::echo %CMD%
%CMD%
