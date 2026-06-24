@echo off
setlocal
javac -encoding UTF-8 -d target\test-classes -cp @test-cp.txt src\test\java\com\cloudhub\platform\space\service\RoomPurposeServiceTest.java
echo RoomPurpose Exit: %errorlevel%
echo.
javac -encoding UTF-8 -d target\test-classes -cp @test-cp.txt src\test\java\com\cloudhub\platform\space\service\KitServiceTest.java
echo KitService Exit: %errorlevel%