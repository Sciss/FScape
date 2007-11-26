#!/bin/sh

followlink()
{
	prg="$1"
	while [ -h "$prg" ] ; do
		ls=`ls -ld "$prg"`
		link=`expr "$ls" : '.*-> \(.*\)$'`
		if expr "$link" : '.*/.*' > /dev/null; then
			prg="$link"
		else
			prg=`dirname "$prg"`/"$link"
		fi
	done
	echo $prg
}

absdir() 
{ 
	[ -n "$1" ] && ( cd "$1" 2> /dev/null && pwd ; ) 
}

where=`followlink $0`
where=`dirname ${where}`
where=`absdir ${where}`
cd ${where}

java -Dapple.laf.useScreenMenuBar=true -Xmx512m -ea -cp "build/FScape.jar:libraries/MRJAdapter.jar:libraries/NetUtil.jar" de.sciss.fscape.Main $@
