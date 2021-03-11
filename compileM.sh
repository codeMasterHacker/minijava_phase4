jar xf vapor-parser.jar
javac -Xlint -classpath vapor-parser.jar VM2M.java VaporMipsVisitor.java
java VM2M < /Users/Enrique/Desktop/phase4/Phase4Tests/LinearSearch.vaporm > P.s