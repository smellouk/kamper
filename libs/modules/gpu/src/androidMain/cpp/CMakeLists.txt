cmake_minimum_required(VERSION 3.22.1)
project("konitor_gpu_fdinfo")

add_library(konitor_gpu_fdinfo SHARED konitor_fdinfo.c)

target_link_libraries(konitor_gpu_fdinfo log)
