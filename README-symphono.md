Openfire
========

Symphono specific setup instructions for Openfire Eclipse project
-----------------------------------------------------------------

In order to get the repo checked out and setup in Eclipse, there are a couple of things to do and make sure:

1. Clone the repo
2. Copy the following:
  1. cd <repo>
  2. cp -rp build/eclipse/settings .settings
  3. cp -p build/eclipse/project .project
  4. cp -p build/eclipse/classpath .classpath
3. Open Eclipse and import existing maven project
4. Make sure you have 'ANT' installed and that your 'ANT_HOME' env var is pointing to the correct directory
5. Make sure you have your JAVA_HOME set correctly to point to the JDK 1.7
6. Then try to run a maven clean and maven install from Eclipse.
