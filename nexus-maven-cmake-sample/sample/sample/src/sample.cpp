#include "sample.h"
#include <modulea/modulea.h>

#include <QDebug>

Sample::Sample(QWidget *parent, Qt::WindowFlags flags)
    : QMainWindow(parent, flags)
{
    ui.setupUi(this);

    moduleA module;
    //qWarning() << module.getValue();
    //ui.centralWidget->setToolTip(module.getValue());
    qWarning() << module.getValueFromModuleB();
    ui.centralWidget->setToolTip(module.getValueFromModuleB());
}

Sample::~Sample()
{

}
