#ifndef MODULEA_H
#define MODULEA_H

#include <modulea/modulea_global.h>

class MODULEA_EXPORT moduleA
{
public:
    moduleA();
    ~moduleA();

    QString getValue() const;

    QString getValueFromModuleB() const;

private:

};

#endif // MODULEA_H
