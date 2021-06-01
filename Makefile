all: compile

compile:
	java -jar ./jar/jtb132di.jar -te minijava.jj
	java -jar ./jar/javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	find ./*.java | grep -v "Main.java" | xargs rm
	rm -f *.class *~ ./types/*.class ./visitors/*.class
	rm -rf syntaxtree
	rm -rf visitor

