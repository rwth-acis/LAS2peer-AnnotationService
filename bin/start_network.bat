:: this script starts a las2peer node providing the example service of this project
:: pls execute it from the bin folder of your deployment by double-clicking on it

cd ..
set BASE=%CD%
set CLASSPATH="%BASE%/lib/*;"

java -cp %CLASSPATH% i5.las2peer.tools.L2pNodeLauncher -w -p 9011 uploadStartupDirectory('etc/startup') startService('i5.las2peer.services.annotations.AnnotationsService','test') startWebConnector interactive
pause
