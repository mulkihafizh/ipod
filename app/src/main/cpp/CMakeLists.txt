cmake_minimum_required(VERSION 3.22.1)
project("retro_ipod_audio")

find_package(oboe REQUIRED CONFIG)

add_library(retro_ipod_audio SHARED
    native_audio.cpp
)

target_link_libraries(retro_ipod_audio
    oboe::oboe
    log
    android
)
