export CP=`cat ~/gits/spring-framework/debug/spring-core/cp.txt | grep "^/" | tr '\n' ':'`
export CP=$CP:junit-platform-console-standalone-1.5.2.jar
export CP=$CP:./build/classes/java/test:./build/classes/java/main:./build/resources/test

