@echo off
:: palisades.lakes (at) gmail (dot) com
:: 2024-01-15

::set GC=-XX:+AggressiveHeap -XX:+UseStringDeduplication 
set GC=

set TRACE=
::set TRACE=-XX:+PrintGCDetails -XX:+TraceClassUnloading -XX:+TraceClassLoading

::set THRUPUT=
set THRUPUT=-server -Xbatch -XX:+UseFMA
::set THRUPUT=-server -Xbatch 

::set XMX=-Xms48g -Xmx48g -Xmn16g 
::set XMX=-Xms31g -Xmx31g -Xmn12g 
set XMX=-Xms8g -Xmx8g -Xmn3g

::set PROF=
set PROF=-agentpath:"C:\Program Files\YourKit Java Profiler 2023.9-b104\bin\windows-x86-64\yjpagent.dll=_no_java_version_check"

set OPENS=--add-opens java.base/java.lang=ALL-UNNAMED
set CP=-cp lib/*

::set JAVA_HOME=%JAVA16%
set JAVA="%JAVA_HOME%\bin\java"

set CMD=%JAVA% %THRUPUT% -ea -dsa  --enable-preview %PROF% %GC% %XMX% %TRACE% %OPENS% %CP% %*
::echo %CMD%
%CMD%
