.PHONY: compile run

compile: pom.xml
	mvn package

run: pom.xml
	mvn compile && mvn exec:java -Dexec.mainClass="edu.pntalk.sequencer.Launcher"

run2: pom.xml
	mvn package
	java --module-path=./lib/javafx-sdk-11.0.2/lib/ --add-modules=javafx.controls,javafx.graphics -jar target/sequencer-maven-project-1.0-jar-with-dependencies.jar

	