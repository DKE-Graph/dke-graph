cmake_minimum_required(VERSION 3.16)

project(myRDMA)
set(CMAKE_CXX_COMPILER "/usr/bin/g++")
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/src/network)

add_executable (myRDMA ${CMAKE_CURRENT_SOURCE_DIR}/main.cpp)

target_link_libraries(myRDMA PRIVATE network)
target_compile_features(myRDMA PRIVATE cxx_std_17)
