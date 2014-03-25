QT       += core
QT       += network
QT       -= gui

TARGET = wallet-server
CONFIG   += console
CONFIG   -= app_bundle
CONFIG   += c++11

TEMPLATE = app


SOURCES += main.cpp \
    server/synclistener.cpp \
    sync/sync_protocol.pb.cc \
    server/synctcpserver.cpp

HEADERS += \
    server/synclistener.h \
    server/synctcpserver.h
    sync/sync_protocol.pb.h


unix {
    CONFIG += link_pkgconfig
    PKGCONFIG += protobuf
}
