@echo off
:: palisades dot lakes at gmail dot com
:: 2026-11-22


::set THRUPUT=-d64 -server -XX:+AggressiveOpts
set THRUPUT=-d64 -server
::set THRUPUT=

set UNWARN=--enable-native-access=ALL-UNNAMED --enable-native-access=javafx.graphics --sun-misc-unsafe-memory-access=allow

set XMX=
::set XMX=-Xms29g -Xmx29g -Xmn11g
::set XMX=-Xms48g -Xmx48g -Xmn16g

set OPENS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/javax.imageio=ALL-UNNAMED --add-opens java.desktop/javax.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED

set CP=-cp ./src/scripts/clojure;lib/*

:: TODO: safer way of serializing arbitrary clojure functions built thru calls to fn
:: see https://clojure.org/news/2026/11/18/clojure-1-12-7
set SERIALIZED_FNS=-Dclojure.serializable.fns=true

::set JFX_VERSION=27.0.1.1
set JFX_VERSION=25.0.5.1

set MODULE_PATH=--module-path lib\javafx-base-%JFX_VERSION%-win.jar;lib\javafx-base-%JFX_VERSION%.jar;lib\javafx-controls-%JFX_VERSION%-win.jar;lib\javafx-controls-%JFX_VERSION%.jar;lib\javafx-graphics-%JFX_VERSION%-win.jar;lib\javafx-graphics-%JFX_VERSION%.jar;lib\javafx-media-%JFX_VERSION%-win.jar;lib\javafx-media-%JFX_VERSION%.jar;lib\javafx-web-%JFX_VERSION%-win.jar;lib\javafx-web-%JFX_VERSION%.jar
set MODULES=--add-modules javafx.controls,javafx.media,javafx.web
set UPGRADE_PATH=--upgrade-module-path lib\jdk-jsobject-27.0.1.1.jar;lib\jdk-jsobject-27.0.1.1-win.jar

set JAVA="%JAVA_HOME%\bin\java"

set CLJOPTS=-Dclojure.main.report=stderr
set CMD=%JAVA% %UNWARN% -ea %GC% %XMX% %TRACE% %SERI*ALIZED_FNS% %OPENS% %CP% %MODULE_PATH% %UPGRADE_PATH% %UNWARN% %MODULES% %CLJOPTS% clojure.main %*
::echo %CMD%
%CMD%

