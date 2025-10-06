@echo off
:: palisades dot lakes at gmail dot com
:: 2021-04-20

::set GC=-XX:+AggressiveHeap -XX:+UseStringDeduplication 
set GC=

set TRACE=
::set TRACE=-XX:+PrintGCDetails -XX:+TraceClassUnloading -XX:+TraceClassLoading

set UNWARN=--enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow

set XMX=
::set XMX=-Xms29g -Xmx29g -Xmn11g 
;;set XMX=-Xms12g -Xmx12g -Xmn5g 

set OPENS=--add-opens java.base/java.lang=ALL-UNNAMED
set CP=-cp ./src/scripts/clojure;lib/*
set JAVA="%JAVA_HOME%\bin\java"

set CLJOPTS=-Dclojure.main.report=stderr
set CMD=%JAVA% %UNWARN% -ea %GC% %XMX% %TRACE% %OPENS% %CP% %CLJOPTS% clojure.main %*
::echo %CMD%
%CMD%
