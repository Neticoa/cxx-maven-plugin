#include <moduleA/modulea.h>

#include <moduleB/moduleb.h>

#include <QString>

moduleA::moduleA()
{

}

moduleA::~moduleA()
{

}

QString moduleA::getValue() const
{
    return "module A value";
}

QString moduleA::getValueFromModuleB() const
{
    return ModuleB().getValue();
}