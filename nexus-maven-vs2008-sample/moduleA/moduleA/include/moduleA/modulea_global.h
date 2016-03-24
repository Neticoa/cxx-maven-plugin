#ifndef MODULEA_GLOBAL_H
#define MODULEA_GLOBAL_H

#include <QtCore/qglobal.h>

#ifdef MODULEA_LIB
# define MODULEA_EXPORT Q_DECL_EXPORT
#else
# define MODULEA_EXPORT Q_DECL_IMPORT
#endif

#endif // MODULEA_GLOBAL_H
