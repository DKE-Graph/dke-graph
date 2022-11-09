cmake_minimum_required(VERSION 3.16)

project(RDMAtest)
set(CMAKE_CXX_COMPILER "/usr/bin/g++")
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/src/network)

add_executable (k8s_rdma_test ${CMAKE_CURRENT_SOURCE_DIR}/main.cpp)

target_link_libraries(k8s_rdma_test PRIVATE network)
target_compile_features(k8s_rdma_test PRIVATE cxx_std_17)