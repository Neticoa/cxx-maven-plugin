#!/bin/sh
# exec-unit-tests.sh generic Qt Unit Test runner
#############################################################################
# 0/ Where are we ?
OPTION_DISPLAY=":0.0"
X11=`which X`
if [ "$?" != "0" ]; then
	X11=""
fi

OFF=`uname -a | grep -i 'linux'` 
if [ "$?" = "0" ]; then
	OPTION_DISPLAY=":0.0"
else
	OFF=`uname -a | grep -i 'cygwin'`
	if [ "$?" = "0" ]; then
		X11="Windows"
		OPTION_DISPLAY=""
	else
		OFF=`uname -a | grep -i 'darwin'`
		if [ "$?" = "0" ]; then
			X11="Aqua"
			OPTION_DISPLAY=""
		else
			OPTION_DISPLAY=""
		fi
	fi
fi
echo "Graphical system detected : $X11"

EXECUTABLEPATH=".\-PROJECT-TESTS"
REPORTFILEPATH=".\TEST-result-.xml"

if [ $# -ge 1 ]; then
	EXECUTABLEPATH=$1
fi
if [ $# -ge 2 ]; then
	REPORTFILEPATH=$2
fi

# create full report path location
echo "Create directiory `echo "$REPORTFILEPATH" | sed -E "s/(.*\/).*/\1/"`"
mkdir -p "`echo "$REPORTFILEPATH" | sed -E "s/(.*\/).*/\1/"`"

echo Execute "$EXECUTABLEPATH" -xunitxml -o "$REPORTFILEPATH"
"$EXECUTABLEPATH" -xunitxml -o "$REPORTFILEPATH"
