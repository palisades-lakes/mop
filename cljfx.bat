@echo off
:: palisades dot lakes at gmail dot com
:: 206-04-05


set TRACE=
::set TRACE=-XX:+PrintGCDetails -XX:+TraceClassUnloading -XX:+TraceClassLoading

set UNWARN=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow

set XMX=
::set XMX=-Xms29g -Xmx29g -Xmn11g
::set XMX=-Xms48g -Xmx48g -Xmn16g

set OPENS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/javax.imageio=ALL-UNNAMED --add-opens java.desktop/javax.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED

set CP=-cp ./src/scripts/clojure;lib/*
::set CP=-cp lib/*

set MODULE_PATH=--module-path lib\javafx-base-25-win.jar;lib\javafx-base-25.jar;lib\javafx-controls-25-win.jar;lib\javafx-controls-25.jar;lib\javafx-graphics-25-win.jar;lib\javafx-graphics-25.jar;lib\javafx-media-25-win.jar;lib\javafx-media-25.jar;lib\javafx-web-25-win.jar;lib\javafx-web-25.jar
set MODULES=--add-modules javafx.base,javafx.controls,javafx.graphics,javafx.media,javafx.web
set JAVA="%JAVA_HOME%\bin\java"

set CLJOPTS=-Dclojure.main.report=stderr
set CMD=%JAVA% %UNWARN% -ea %GC% %XMX% %TRACE% %OPENS% %CP% --sun-misc-unsafe-memory-access=allow --enable-native-access=javafx.graphics %MODULE_PATH% %MODULES% %CLJOPTS% clojure.main %*
::echo %CMD%
%CMD%

