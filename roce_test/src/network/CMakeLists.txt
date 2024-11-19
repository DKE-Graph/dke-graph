file(GLOB_RECURSE SRC_FILES CONFIGURE_DEPENDS
  ${CMAKE_CURRENT_SOURCE_DIR}/*.cpp
)

add_library(network STATIC ${SRC_FILES})

target_include_directories(network PUBLIC ../../includes/network)

target_link_libraries(network PRIVATE pthread ibverbs)


