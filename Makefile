# Author: Erik Kelemen <xkelem01@stud.fit.vutbr.cz>

.PHONY: compile run docs

docs: pom.xml
	mvn dokka:dokka

run: pom.xml
	java --module-path=./lib/javafx-sdk-11.0.2/lib/ --add-modules javafx.controls,javafx.graphics --add-opens javafx.graphics/javafx.scene.text=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.scene.text=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.geom=ALL-UNNAMED --add-opens javafx.graphics/com.sun.javafx.text=ALL-UNNAMED --add-exports javafx.graphics/com.sun.javafx.text=ALL-UNNAMED -jar target/sequencer-maven-project-1.0-jar-with-dependencies.jar

compile: pom.xml
	mvn package
	jar ufm target/sequencer-maven-project-1.0-jar-with-dependencies.jar manifest.txt
