@echo off
rem One-shot launcher for CameraTestRunner on Windows.
rem
rem Builds the test classpath via maven, then runs the standard set of
rem camera-required tests (or the classes named on the command line) in a
rem JVM with -DcanonCameraConnected=true. The Windows EDSDK.dll has no
rem Cocoa dependency, so this is just a convenience over `mvn test`.

setlocal

set "ROOT=%~dp0"
set "CP_FILE=%TEMP%\canon-sdk-java-cp.txt"

call "%ROOT%mvnw.cmd" -am -pl camera-framework ^
    dependency:build-classpath ^
    -Dmdep.includeScope=test ^
    -Dmdep.outputFile="%CP_FILE%" ^
    -DskipTests
if errorlevel 1 goto fail

set /p DEPS_CP=<"%CP_FILE%"
set "CP=%ROOT%camera-framework\target\test-classes;%ROOT%camera-framework\target\classes;%ROOT%camera-binding\target\classes;%DEPS_CP%"

java -cp "%CP%" -DcanonCameraConnected=true ^
    org.blackdread.cameraframework.demo.CameraTestRunner %*
exit /b %errorlevel%

:fail
echo Maven step failed; aborting.
exit /b 1
