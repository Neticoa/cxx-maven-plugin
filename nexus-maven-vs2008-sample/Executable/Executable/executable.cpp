#include "executable.h"
#include <moduleA/moduleA.h>

#include <QDebug>

Executable::Executable(QWidget *parent, Qt::WFlags flags)
    : QMainWindow(parent, flags)
{
    ui.setupUi(this);

    moduleA module;
    qWarning() << module.getValueFromModuleB();
    ui.centralWidget->setToolTip(module.getValueFromModuleB());
}

Executable::~Executable()
{

}
