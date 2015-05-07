package org.di.api.impl.carfax

import org.junit.Ignore
import org.junit.Test

class StringMajorMinorPatchVersionTest {
    @Test
    void testComparisonMajorVersions() {
        StringMajorMinorPatchVersion smaller = new StringMajorMinorPatchVersion("1.0.0")
        StringMajorMinorPatchVersion bigger = new StringMajorMinorPatchVersion("2.0.0")
        assert smaller.before(bigger)
        assert bigger.after(smaller)
    }

    @Test
    void testEquals() {
        StringMajorMinorPatchVersion one = new StringMajorMinorPatchVersion("10.34.99")
        StringMajorMinorPatchVersion two = new StringMajorMinorPatchVersion("10.34.99")
        assert !one.before(two)
        assert !two.after(one)
        assert one == two
    }

    @Test
    void doYourBestWithSnapshots() {
        StringMajorMinorPatchVersion snapshot = new StringMajorMinorPatchVersion("2.3.4-SNAPSHOT")
        assert snapshot.toString() == "2.3.3"

        StringMajorMinorPatchVersion snapshot2 = new StringMajorMinorPatchVersion("2.3.0-SNAPSHOT")
        assert snapshot2.toString() == "2.2.0"
    }
}