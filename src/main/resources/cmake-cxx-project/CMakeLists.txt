########################################################################################
########################################################################################
# A TYPICAL MAVENIZED CMAKE PROJECT
# see https://cmake.org/cmake/help/v3.0/
########################################################################################
########################################################################################

cmake_minimum_required(VERSION 3.0.0 FATAL_ERROR)

########################################################################################
# TARGET INITIALISATION

get_directory_property(hasParent PARENT_DIRECTORY)

# MAVEN Mandatory. TARGET_NAME can be named what ever you want 
if (hasParent)
    message(WARNING "TARGET_NAME reset since current project has parent")
    unset(TARGET_NAME PARENT_SCOPE)
    unset(TARGET_NAME CACHE)
endif()
if(NOT TARGET_NAME)
    if (NOT hasParent)
        message(WARNING "Please provide a componant name using TARGET_NAME properties")
    endif()
    set(TARGET_NAME "Sample") # default TARGET_NAME come from cxx-maven plugin generator
endif()

# MAVEN Mandatory. TARGET_ID can be what ever you want 
# (not necessary same as project name) but need to be lower case and
# usualy used as project componant source root
if (hasParent)
    message(WARNING "TARGET_ID reset since current project has parent")
    unset(TARGET_ID PARENT_SCOPE)
    unset(TARGET_ID CACHE)
endif()
if(NOT TARGET_ID)
    if (NOT hasParent)
        message(WARNING "Please provide a componant id using TARGET_ID properties")
    endif()
    set(TARGET_ID "sample") # default TARGET_ID come from cxx-maven plugin generator
endif()

# MAVEN Mandatory. DEPENDENCY_DIR point to target/dependency
if(NOT DEPENDENCY_DIR)
    set(DEPENDENCY_DIR "${PROJECT_SOURCE_DIR}")
    message(WARNING "default DEPENDENCY_DIR is ${DEPENDENCY_DIR}")
endif()

# MAVEN Mandatory. TARGET_VERSION is pom version
if(NOT TARGET_VERSION)
    message(WARNING "Please provide a componant version using TARGET_VERSION properties")
    set(TARGET_VERSION "0.0.0.1") # default TARGET_VERSION come from cxx-maven plugin generator
endif()
add_definitions(-DTARGET_VERSION=${TARGET_VERSION})

# MAVEN Mandatory. TARGET_PLATFORM is setted by maven (or you !)
if(NOT TARGET_PLATFORM)
    # ${CMAKE_SYSTEM_NAME} is not safe !
    message(FATAL_ERROR "Please provide a target platform name using TARGET_PLATFORM properties {win32, win64, linux, mac}")
endif()
add_definitions(-DTARGET_PLATFORM=${TARGET_PLATFORM})

# MAVEN Mandatory. TARGET_ARCHITECTURE is setted by maven (or you !)
if(NOT TARGET_ARCHITECTURE)
    # there is no safe way to set a target architecture under cmake
    message(FATAL_ERROR "Please provide a target architecture using TARGET_ARCHITECTURE properties {x86, amd64, i386, i686, x86_64}")
endif()
add_definitions(-DTARGET_ARCHITECTURE=${TARGET_ARCHITECTURE})

# MAVEN Mandatory. TARGET_CLASSIFIER is setted by maven (or you !)
if(NOT TARGET_CLASSIFIER)
    # target classifier is a conjonction of target platform and target architecture
    message(FATAL_ERROR "Please provide a target classifier using TARGET_CLASSIFIER properties {win32, win64, linux-i686, linux-x86_64, mac-i386, mac-x86_64}")
endif()
add_definitions(-DTARGET_CLASSIFIER=${TARGET_CLASSIFIER})

# CMAKE Mandatory.
project(${TARGET_ID} VERSION ${TARGET_VERSION})

# MAVEN Mandatory. CMakeMavenDependencies.txt file define a cmake function 
# named "add_maven_dependencies" that you shall call on your targets.
# This function is generated and updated by maven according to current project
# dependencies declared in pom.xml.
# see https://cmake.org/cmake/help/v3.0/command/include.html
#
# CMAKE DEPENDENCIES MANAGMENT integration with MAVEN DEPENDENCIES MANAGMENT
# Following include try to find CMakeMavenDependencies.txt at source root dir. This is because 
# CMakeMavenDependencies.txt is produced by maven and it is implicitly associated to root CMakeLists.txt context 
# In other words :
# - If you try to build an amalgamed cmake project, this avoid injecting bad dependency from previously
# compiled standalone cmake/maven project but will inject global dependencies define at amalgamed project level.
# - If you try to build an standalone cmake/maven project, this inject the right dependencies, those
# define in pom.xml located at this level
include("${CMAKE_SOURCE_DIR}/CMakeMavenDependencies.txt" OPTIONAL RESULT_VARIABLE MAVEN_DEPENDENCIES)
# Following include try to find CMakenDependencies.txt at this level dir.  
# CMakeDependencies.txt is also produced by maven and it is implicitly associated to this CMakeLists.txt context. 
# It contains a cmake amalgamed context compliant version of maven dependencies
include("CMakeDependencies.txt" OPTIONAL RESULT_VARIABLE CMAKE_DEPENDENCIES)

# CMAKE Mandatory. Project content definition setted by you !
include(CMakeProjectContent.txt)

# MAVEN Mandatory. MAVEN DEPENDENCIES MANAGMENT Default behavior : add maven dependancies 
# to "supposed" defaut cmake target of current cmake project
if (TARGET "${TARGET_NAME}" AND NOT "${MAVEN_DEPENDENCIES}" STREQUAL "NOTFOUND" )
    add_maven_dependencies(${TARGET_NAME})
endif()

# MAVEN Mandatory. CMAKE DEPENDENCIES MANAGMENT Default behavior : add cmake dependancies 
# to "supposed" defaut cmake target of current cmake project
if (TARGET "${TARGET_NAME}" AND NOT "${CMAKE_DEPENDENCIES}" STREQUAL "NOTFOUND" )
    add_cmake_dependencies(${TARGET_NAME})
endif()

###################################################################################
# TWEAK LD_LIBRARY_PATH
# see 'otool -L', 'otool -D', 'install_name_tool -id' and 'install_name_tool -change' under Mac OS X
# see 'ldd', 'readelf -d' and 'chrpath' under Linux
# there is nothing to do under windows

if( NOT COMMAND APPEND_CMAKE_INSTALL_RPATH)
    macro(APPEND_CMAKE_INSTALL_RPATH RPATH_DIRS)
        if( NOT ${ARGC} EQUAL 1)
            message(SEND_ERROR "APPEND_CMAKE_INSTALL_RPATH takes 1 argument")
        endif()
        foreach( RPATH_DIR ${RPATH_DIRS} )
            if( NOT ${RPATH_DIR} STREQUAL "" )
                file( TO_CMAKE_PATH ${RPATH_DIR} RPATH_DIR )
                string( SUBSTRING ${RPATH_DIR} 0 1 RPATH_FIRST_CHAR )
                if( NOT ${RPATH_FIRST_CHAR} STREQUAL "/" )
                    # relative path; CMake handling for these is unclear,
                    # add them directly to the linker line. Add both $ORIGIN
                    # and $$ORIGIN to ensure correct behavior for exes and
                    # shared libraries.
                    set( RPATH_DIR "$ORIGIN/${RPATH_DIR}:$$ORIGIN/${RPATH_DIR}" )
                    set( CMAKE_EXE_LINKER_FLAGS
                          "${CMAKE_EXE_LINKER_FLAGS} -Wl,-rpath,'${RPATH_DIR}'" )
                    set( CMAKE_SHARED_LINKER_FLAGS
                          "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-rpath,'${RPATH_DIR}'" )
                else()
                    # absolute path
                    set( CMAKE_INSTALL_RPATH "${CMAKE_INSTALL_RPATH}:${RPATH_DIR}" )
                endif()
            endif()
        endforeach()
    endmacro()
endif()

if(${TARGET_PLATFORM} MATCHES "linux")
    # Is this stuff required for ELF binary format (or only for gcc ?)

    # don't skip the full RPATH for the build tree
    SET(CMAKE_SKIP_BUILD_RPATH  FALSE)
    # when building, use install RPATH asap
    SET(CMAKE_BUILD_WITH_INSTALL_RPATH TRUE)
    # Tell that dynamic library are to search in current directory at execution time
    APPEND_CMAKE_INSTALL_RPATH(".")
    # add relative path of project libs depends to RPATH
    SET(CMAKE_INSTALL_RPATH_USE_LINK_PATH TRUE)
endif()
