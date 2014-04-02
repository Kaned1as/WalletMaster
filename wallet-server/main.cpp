/**************************************************************************
** This program is free software; you can redistribute it and/or
** modify it under the terms of the GNU General Public License as
** published by the Free Software Foundation; either version 3 of the
** License, or (at your option) any later version.
**
** This program is distributed in the hope that it will be useful, but
** WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
** General Public License for more details:
** http://www.gnu.org/licenses/gpl.txt
**************************************************************************/

#include <QCoreApplication>
#include <QTimer>
#include <QDir>

#include "server/synclistener.h"
#include "google/protobuf/stubs/common.h"

int main(int argc, char *argv[])
{
    GOOGLE_PROTOBUF_VERIFY_VERSION;

    QCoreApplication a(argc, argv);

    SyncListener syncer(&a);
    QObject::connect(&a, &QCoreApplication::aboutToQuit, &syncer, &SyncListener::stop);

    QTimer::singleShot(0, &syncer, SLOT(start()));
    //QTimer::singleShot(2000, &a, SLOT(quit()));

    return a.exec();
}
