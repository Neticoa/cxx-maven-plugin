#ifndef EXECUTABLE_H
#define EXECUTABLE_H

#include <QtGui/QMainWindow>
#include "ui_executable.h"

class Executable : public QMainWindow
{
    Q_OBJECT

public:
    Executable(QWidget *parent = 0, Qt::WFlags flags = 0);
    ~Executable();

private:
    Ui::ExecutableClass ui;
};

#endif // EXECUTABLE_H
