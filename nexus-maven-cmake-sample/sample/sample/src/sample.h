#ifndef EXECUTABLE_H
#define EXECUTABLE_H

#include <QMainWindow>
#include "ui_sample.h"

class Sample : public QMainWindow
{
    Q_OBJECT

public:
    Sample(QWidget *parent = 0, Qt::WindowFlags flags = 0);
    ~Sample();

private:
    Ui::SampleClass ui;
};

#endif // EXECUTABLE_H
