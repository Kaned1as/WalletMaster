WalletMaster, GPLv3
===================

Money management software that primary focus is synchronization.

Main application is android client (Dalvik/Java). All the processing and conflict merge after sync is done here. Its' build system is as usual, Gradle+Android Studio.
If you are familiar with android app development, it will be no problem for you to use it.

Server side is written in Qt (C++11) and with Mysql server in mind. It serves primarily only as storage for client data.

Communication between server and client is done via Google's protobuf libraries. Current protocol version is stored in sync_protocol.proto file.
