cmake_minimum_required(VERSION 3.22.1)
project("kamper_gpu_fdinfo")

add_library(kamper_gpu_fdinfo SHARED kamper_fdinfo.c)

target_link_libraries(kamper_gpu_fdinfo log)
