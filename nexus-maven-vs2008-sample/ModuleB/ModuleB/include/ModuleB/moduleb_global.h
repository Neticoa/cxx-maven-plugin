#ifndef MODULEB_GLOBAL_H
#define MODULEB_GLOBAL_H

#include <QtCore/qglobal.h>

#ifdef MODULEB_LIB
# define MODULEB_EXPORT Q_DECL_EXPORT
#else
# define MODULEB_EXPORT Q_DECL_IMPORT
#endif

#endif // MODULEB_GLOBAL_H
