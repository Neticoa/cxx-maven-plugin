#include <QtTest/QtTest>
#include <utils/utils.h>
#include "SAMPLE-test.h"

SAMPLEtest::SAMPLEtest()
{
}

void SAMPLEtest::testCase1()
{
    utils();
    QVERIFY2(2 == 1 + 1, "math Failure");
}
