.PHONY: compile run

compile: pom.xml
	mvn compile

run: pom.xml
	mvn compile && mvn exec:java -Dexec.mainClass="edu.pntalk.sequencer.Launcher"