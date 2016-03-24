#include "sample.h"
#include <QApplication>

int main(int argc, char *argv[])
{
    QApplication a(argc, argv);
    Sample w;
    w.show();
    return a.exec();
}
