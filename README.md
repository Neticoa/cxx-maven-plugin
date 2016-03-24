# Introduction to Cxx Maven Plugin
* This plugin contains various goals to assist C++ compilation using the Maven build lifecycle.

## [Goals Overview](http://neticoa.github.io/cxx-maven-plugin/plugin-info.html)
* [cxx-maven-plugin:addsource](http://neticoa.github.io/cxx-maven-plugin/addsource-mojo.html) add multiple source tree to your project
* [cxx-maven-plugin:launch](http://neticoa.github.io/cxx-maven-plugin/launch-mojo.html) generic command launch goal
* [cxx-maven-plugin:cmake](http://neticoa.github.io/cxx-maven-plugin/cmake-mojo.html) project generation goal using cmake tool.
* [cxx-maven-plugin:qmake](http://neticoa.github.io/cxx-maven-plugin/qmake-mojo.html) project generation goal using qmake tool.
* [cxx-maven-plugin:make](http://neticoa.github.io/cxx-maven-plugin/make-mojo.html) build goal using make tool.
* [cxx-maven-plugin:msbuild](http://neticoa.github.io/cxx-maven-plugin/msbuild-mojo.html) build goal using visual studio.
* [cxx-maven-plugin:generate](http://neticoa.github.io/cxx-maven-plugin/generate-mojo.html) Generates a new project from an archetype, or updates the actual project if using a partial archetype. If the project is generated or updated in the current directory. mvn cxx:generate -DartifactName="an-id" -DartifactId="AnId".
* [cxx-maven-plugin:scm-dependencies](http://neticoa.github.io/cxx-maven-plugin/scm-dependencies-mojo.html)	Goal that retrieve source dependencies from the SCM ie. This is an enhanced version of [maven-dependencies-plugin:unpack-dependencies](https://maven.apache.org/plugins/maven-dependency-plugin/unpack-dependencies-mojo.html) capable to drive SCM tools (SVN for now). "Let maven manage your svn:externals".
* [cxx-maven-plugin:unpack-dependencies](http://neticoa.github.io/cxx-maven-plugin/unpack-dependencies-mojo.html) Goal that unpacks the project dependencies from the repository to a defined location. This is an enhenced version of [maven-dependencies-plugin:unpack-dependencies](https://maven.apache.org/plugins/maven-dependency-plugin/unpack-dependencies-mojo.html) capable to handle c++ build types specificities (debug|release and any mix of them).
* [cxx-maven-plugin:xunit](http://neticoa.github.io/cxx-maven-plugin/xunit-mojo.html) unit test report generic generation goal.
* [cxx-maven-plugin:cppcheck](http://neticoa.github.io/cxx-maven-plugin/cppcheck-mojo.html) cppcheck reports generation goal.
* [cxx-maven-plugin:cppncss](http://neticoa.github.io/cxx-maven-plugin/cppncss-mojo.html) cppncss reports generation goal.
* [cxx-maven-plugin:veraxx](http://neticoa.github.io/cxx-maven-plugin/veraxx-mojo.html) vera++ reports generation goal.
* [cxx-maven-plugin:coverage](http://neticoa.github.io/cxx-maven-plugin/coverage-mojo.html) gcov reports generation goal
* [cxx-maven-plugin:valgrind](http://neticoa.github.io/cxx-maven-plugin/valgrind-mojo.html) valgrind reports generation goal.

## [Usage](http://neticoa.github.io/cxx-maven-plugin/plugin-info.html)
* Instructions on how to use the Cxx Maven Plugin can be found on the [usage page](http://neticoa.github.io/cxx-maven-plugin/plugin-info.html)
* cxx-maven-plugin is intended to integrate seamlessly with [sonar-cxx-plugin](https://github.com/SonarOpenCommunity/sonar-cxx)

## Examples

* [VS sample](https://github.com/Neticoa/cxx-maven-plugin/tree/gh-pages/nexus-maven-vs2008-sample)

* [CMake sample](https://github.com/Neticoa/cxx-maven-plugin/tree/gh-pages/nexus-maven-cmake-sample)

## Old examples
* [pom sample](http://neticoa.github.io/cxx-maven-plugin/SAMPLE-OLD/pom.xml)

* [complete sample](https://github.com/Neticoa/cxx-maven-plugin/tree/gh-pages/SAMPLE-OLD)
