@echo off
:: palisades dot lakes at gmail dot com
:: 2025-10-12

::set GC=-XX:+AggressiveHeap -XX:+UseStringDeduplication 
set GC=

set TRACE=
::set TRACE=-XX:+PrintGCDetails -XX:+TraceClassUnloading -XX:+TraceClassLoading

set UNWARN=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow

;;set XMX=
::set XMX=-Xms29g -Xmx29g -Xmn11g 
set XMX=-Xms48g -Xmx48g -Xmn16g

set OPENS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.desktop/javax.imageio=ALL-UNNAMED --add-opens java.desktop/javax.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.tiff=ALL-UNNAMED --add-opens java.desktop/com.sun.imageio.plugins.png=ALL-UNNAMED

set CP=-cp ./src/scripts/clojure;lib/*
::set CP=-cp lib/*

set JAVA="%JAVA_HOME%\bin\java"

set CLJOPTS=-Dclojure.main.report=stderr
set CMD=%JAVA% %UNWARN% -ea %GC% %XMX% %TRACE% %OPENS% %CP% %CLJOPTS% clojure.main %*
::echo %CMD%
%CMD%

