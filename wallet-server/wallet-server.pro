QT       += core
QT       += network
QT       += sql
QT       -= gui

TARGET = wallet-server
CONFIG   += console
CONFIG   -= app_bundle
CONFIG   += c++11

TEMPLATE = app


SOURCES += main.cpp \
    server/synclistener.cpp \
    sync/sync_protocol.pb.cc \
    server/synctcpserver.cpp \
    server/syncclientsocket.cpp

HEADERS += \
    server/synclistener.h \
    server/synctcpserver.h \
    server/syncclientsocket.h \
    sync/sync_protocol.pb.h


unix {
    CONFIG += link_pkgconfig
    PKGCONFIG += protobuf
}

win32 {
    LIBS += $$PWD/lib/libprotobuf.lib
    INCLUDEPATH += $$PWD/../../protobuf-2.5.0/src .
}
