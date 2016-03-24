#
# Copyright (C) 2011-2016, Neticoa SAS France - Tous droits réservés.
# Author(s) : Franck Bonin, Neticoa SAS France
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
########################################################################################
########################################################################################
# A TYPICAL MAVENIZED CMAKE PROJECT
# see https://cmake.org/cmake/help/v3.0/
########################################################################################
########################################################################################

cmake_minimum_required(VERSION 3.0.0 FATAL_ERROR)

########################################################################################
# TARGET INITIALISATION (not requiring project() command)

get_directory_property(hasParent PARENT_DIRECTORY)

# MAVEN Mandatory : TARGET_NAME can be named what ever you want 
if (hasParent)
    message(WARNING "TARGET_NAME reset since current project has parent")
    unset(TARGET_NAME PARENT_SCOPE)
    unset(TARGET_NAME CACHE)
endif()
if(NOT TARGET_NAME)
    if (NOT hasParent)
        message(WARNING "Please provide a componant name using TARGET_NAME properties")
    endif()
    set(TARGET_NAME "ModuleB") # default TARGET_NAME come from cxx-maven plugin generator
endif()

# MAVEN Mandatory : TARGET_ID can be what ever you want 
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
    set(TARGET_ID "moduleb") # default TARGET_ID come from cxx-maven plugin generator
endif()

# MAVEN Mandatory : DEPENDENCY_DIR point to target/dependency
if(NOT DEPENDENCY_DIR)
    if(NOT PROJECT_SOURCE_DIR)
        set(DEPENDENCY_DIR "${CMAKE_SOURCE_DIR}")
    else()
        set(DEPENDENCY_DIR "${PROJECT_SOURCE_DIR}")
    endif()
    message(WARNING "default DEPENDENCY_DIR is ${DEPENDENCY_DIR}")
endif()

# MAVEN Mandatory : TARGET_VERSION is pom version
if(NOT TARGET_VERSION)
    message(WARNING "Please provide a componant version using TARGET_VERSION properties")
    set(TARGET_VERSION "0.0.0.1") # default TARGET_VERSION come from cxx-maven plugin generator
endif()
add_definitions(-DTARGET_VERSION=${TARGET_VERSION})

########################################################################################
# TARGET INITIALISATION (requiring project() command)

# CMAKE Mandatory : define project
project(${TARGET_ID} VERSION ${TARGET_VERSION})


# CMAKE Mandatory : detect project target platform and architecture
if(WIN32)
  if(CMAKE_SIZEOF_VOID_P EQUAL 8)
    set(CMAKE_TARGET_PLATFORM "win64")
    set(CMAKE_TARGET_ARCHITECTURE "amd64")
    set(CMAKE_TARGET_CLASSIFIER "win64")
  else()
    set(CMAKE_TARGET_PLATFORM "win32")
    set(CMAKE_TARGET_ARCHITECTURE "x86")
    set(CMAKE_TARGET_CLASSIFIER "win32")
  endif()
elseif(UNIX AND NOT APPLE)
  if(CMAKE_SYSTEM_NAME MATCHES ".*Linux")
    set(CMAKE_TARGET_PLATFORM "linux")
    if(CMAKE_SIZEOF_VOID_P EQUAL 8)
      set(CMAKE_TARGET_ARCHITECTURE "x86_64")
      set(CMAKE_TARGET_CLASSIFIER "linux-x86_64")
    else()
      set(CMAKE_TARGET_ARCHITECTURE "i686")
      set(CMAKE_TARGET_CLASSIFIER "linux-i686")
    endif()
#  elseif(CMAKE_SYSTEM_NAME MATCHES "kFreeBSD.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "kNetBSD.*|NetBSD.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "kOpenBSD.*|OpenBSD.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES ".*GNU.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES ".*BSDI.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "DragonFly.*|FreeBSD")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "SYSV5.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "Solaris.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "HP-UX.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "AIX.*")
#  elseif(CMAKE_SYSTEM_NAME MATCHES "Minix.*")
  else()
    message(WARNING "Unknown UNIX '${CMAKE_SYSTEM_NAME}' platform")
  endif()
elseif(APPLE)
  if(CMAKE_SYSTEM_NAME MATCHES ".*Darwin.*" OR CMAKE_SYSTEM_NAME MATCHES ".*MacOS.*")
    set(CMAKE_TARGET_PLATFORM "mac")
    if(CMAKE_SIZEOF_VOID_P EQUAL 8)
      set(CMAKE_TARGET_ARCHITECTURE "x86_64")
      set(CMAKE_TARGET_CLASSIFIER "mac-x86_64")
    else()
      set(CMAKE_TARGET_ARCHITECTURE "i386")
      set(CMAKE_TARGET_CLASSIFIER "mac-i386")
    endif()
  # TODO: iOS?
  else()
    message(WARNING "Unknown APPLE '${CMAKE_SYSTEM_NAME}' platform")
  endif()
#elseif(CMAKE_SYSTEM_NAME MATCHES "BeOS.*")
#elseif(CMAKE_SYSTEM_NAME MATCHES "Haiku.*")
else()
  message(WARNING "Unknown '${CMAKE_SYSTEM_NAME}' platform")
endif()
message("CMake self-detected '${CMAKE_TARGET_PLATFORM}' platform, '${CMAKE_TARGET_ARCHITECTURE}' architecture, '${CMAKE_TARGET_CLASSIFIER}' classifier")

# MAVEN Mandatory : TARGET_PLATFORM is setted by maven (or you !)
if(NOT TARGET_PLATFORM)
    if (NOT CMAKE_TARGET_PLATFORM)
        message(FATAL_ERROR "Please provide a target platform name using TARGET_PLATFORM properties {win32, win64, linux, mac}")
    else()
        set(TARGET_PLATFORM ${CMAKE_TARGET_PLATFORM})
    endif()
endif()
add_definitions(-DTARGET_PLATFORM=${TARGET_PLATFORM})

# MAVEN Mandatory : TARGET_ARCHITECTURE is setted by maven (or you !)
if(NOT TARGET_ARCHITECTURE)
    if (NOT CMAKE_TARGET_ARCHITECTURE)
        message(FATAL_ERROR "Please provide a target architecture using TARGET_ARCHITECTURE properties {x86, amd64, i386, i686, x86_64}")
    else()
        set(TARGET_ARCHITECTURE ${CMAKE_TARGET_ARCHITECTURE})
    endif()
endif()
add_definitions(-DTARGET_ARCHITECTURE=${TARGET_ARCHITECTURE})

# MAVEN Mandatory : TARGET_CLASSIFIER is setted by maven (or you !)
if(NOT TARGET_CLASSIFIER)
    # target classifier is a conjonction of target platform and target architecture
    if (NOT CMAKE_TARGET_CLASSIFIER)
         message(FATAL_ERROR "Please provide a target classifier using TARGET_CLASSIFIER properties {win32, win64, linux-i686, linux-x86_64, mac-i386, mac-x86_64}")
    else()
        set(TARGET_CLASSIFIER ${CMAKE_TARGET_CLASSIFIER})
    endif()
endif()
add_definitions(-DTARGET_CLASSIFIER=${TARGET_CLASSIFIER})

message("CMake build for '${TARGET_PLATFORM}' platform, '${TARGET_ARCHITECTURE}' architecture, '${TARGET_CLASSIFIER}' classifier")

# MAVEN Mandatory : CMakeMavenDependencies.txt file define a cmake function 
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
# - If you try to build a standalone cmake/maven project, this inject the right dependencies, those
# define in pom.xml located at this level
include("${CMAKE_SOURCE_DIR}/CMakeMavenDependencies.txt" OPTIONAL RESULT_VARIABLE MAVEN_DEPENDENCIES)
# Following include try to find CMakenDependencies.txt at this level dir.  
# CMakeDependencies.txt is also produced by maven and it is implicitly associated to this CMakeLists.txt context. 
# It contains a cmake amalgamed context compliant version of maven dependencies
include("CMakeDependencies.txt" OPTIONAL RESULT_VARIABLE CMAKE_DEPENDENCIES)

# CMAKE Mandatory : Project content definition setted by you !
include(CMakeProjectContent.txt)

# MAVEN Mandatory : MAVEN DEPENDENCIES MANAGMENT Default behavior : add maven dependancies 
# to "supposed" defaut cmake target of current cmake project
if (TARGET "${TARGET_NAME}" AND NOT "${MAVEN_DEPENDENCIES}" STREQUAL "NOTFOUND" )
    add_maven_dependencies(${TARGET_NAME})
endif()

# MAVEN Mandatory : CMAKE DEPENDENCIES MANAGMENT Default behavior : add cmake dependancies 
# to "supposed" defaut cmake target of current cmake project
if (TARGET "${TARGET_NAME}" AND NOT "${CMAKE_DEPENDENCIES}" STREQUAL "NOTFOUND" )
    add_cmake_dependencies(${TARGET_NAME})
endif()

###################################################################################
# Tools to tweak LD_LIBRARY_PATH
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
