#include "executable.h"
#include <QtGui/QApplication>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    Executable w;
    w.show();
    return a.exec();
}
