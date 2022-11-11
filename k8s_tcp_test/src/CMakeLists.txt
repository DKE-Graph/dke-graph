# /src/CMakeLists.txt

# 정적 라이브러리 shape 를 만든다.
add_library(tcp STATIC tcp.cpp)

# 해당 라이브러리 컴파일 시 사용할 헤더파일 경로
target_include_directories(tcp PUBLIC ../includes)

# 해당 라이브러리를 컴파일 할 옵션
target_compile_options(tcp PRIVATE -Wall -Werror)

target_link_libraries(tcp PRIVATE pthread)