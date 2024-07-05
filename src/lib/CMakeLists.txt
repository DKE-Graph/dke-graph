file(GLOB_RECURSE SRC_FILES CONFIGURE_DEPENDS
  ${CMAKE_CURRENT_SOURCE_DIR}/*.cpp
)

add_library(lib STATIC ${SRC_FILES})

target_include_directories(lib PUBLIC ../../includes/lib)

target_link_libraries(lib PRIVATE pthread ibverbs)
