package org.dependency.integrator.model.implementation.test

import org.dependency.integrator.model.implementation.utils.DependencyForTesting
import org.dependency.integrator.model.implementation.utils.ProjectSourceForTesting
import org.dependency.integrator.model.implementation.utils.VersionForTesting
import org.junit.Test

class ProjectSourceForTestingTest {
    @Test
    void testGetDependenciesImmutable() {
        def project = new ProjectSourceForTesting({
            name = "myproject"
            depends("another", 1)
        })
        project.dependencies << new DependencyForTesting(projectSourceName: "yetanother")
        assert project.dependencies.size() == 1
        assert !project.dependencies.find {it.projectSourceName == "yetanother"}
        assert project.dependencies.find {it.projectSourceName == "another"}
    }

    @Test
    void testSetDependencyVersion() {
        def project = new ProjectSourceForTesting({
            name = "myproject"
            depends("another", 1)
        })
        def dependencyOrig = project.dependencies.find {it.projectSourceName == "another"}
        project.setDependencyVersion(dependencyOrig, dependencyOrig.version.increment())
        assert project.dependencies.find {"another"}.version.after(dependencyOrig.version)
    }

    @Test
    void testSetDependencyVersionReplacesOldDependencyByName() {
        def project = new ProjectSourceForTesting({
            name = "myproject"
            depends("another", 2)
        })
        def original = project.dependencies.find {"another"}
        def dependency = new DependencyForTesting(projectSourceName: "another", version: new VersionForTesting(value: 1))
        project.setDependencyVersion(dependency, dependency.version)
        assert project.dependencies.findAll {it.projectSourceName == "another"}.size() == 1
        assert original.version.after(project.dependencies.find {it.projectSourceName == "another"}.version)
    }

    @Test
    void testSetDependencyVersionForNewDependency() {
        def project = new ProjectSourceForTesting({
            name = "myproject"
        })

        def dependency = new DependencyForTesting(projectSourceName: "yetanother", version: new VersionForTesting(value: 1))
        assert !project.dependencies.find {it.projectSourceName == "yetanother"}
        project.setDependencyVersion(dependency, dependency.version)
        assert project.dependencies.find {it.projectSourceName == "yetanother"}
    }
}
