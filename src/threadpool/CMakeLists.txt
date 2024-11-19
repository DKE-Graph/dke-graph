file(GLOB_RECURSE SRC_FILES CONFIGURE_DEPENDS
  ${CMAKE_CURRENT_SOURCE_DIR}/*.cpp
)

add_library(threadpool STATIC ${SRC_FILES})

target_include_directories(threadpool PUBLIC ../../includes/threadpool)

target_link_libraries(threadpool PRIVATE pthread ibverbs)
