package org.di.api.impl.carfax

import org.di.api.ProjectSource
import org.di.api.SourceRepository
import org.di.engine.BuildRecord
import org.di.engine.BuildRunner
import org.di.engine.BulkDependencyIncrementer
import org.di.engine.SpanningTreeBuilder
import org.di.graph.Edge
import org.di.graph.Graph
import org.di.graph.Node
import org.di.graph.visualization.GraphVizGenerator


public class Main {
    public static void main(String... args) {
        SourceRepository repository = new CarfaxLibSourceRepository(localDir: new File("D:/hackathon"));

        updateOneProject(repository, "dealerautoreports-commons")
    }

    static updateOne(String projectName, Collection<ProjectSource> projects) {
        BulkDependencyIncrementer b = new BulkDependencyIncrementer(projectSource: projects.find {it.name == projectName}, projectSources:  projects)
        b.increment()
    }

    static updateLevel(SourceRepository repository, int rank) {
        Map<ProjectSource, BulkDependencyIncrementer> updates = [:]
        Graph g = new Graph(repository)
        g.initRank()
        g.nodes.findAll {it.rank == rank && it.outgoing.find{it.isStale()}}.each { Node node ->
           println node.projectSource.name
           node.outgoing.findAll {it.stale}.each { Edge edge ->
               println "   "+edge.dependency.projectSourceName + "  "+edge.dependency.version+" ("+edge.to.projectSource.version+")"

           }
           def update = new BulkDependencyIncrementer(node: node)
           updates[node.projectSource] = update
        }
        BuildRunner br = new BuildRunner(projectSources: updates.keySet())
        br.start(4)
        List<BuildRecord> results = br.completeBuildRecords
        def failedBeforeUpdate = results.findAll {it.result == BuildRecord.BuildResult.Failed}.collect {it.projectSource}
        println "Failed before upgrade: "+failedBeforeUpdate
        Map<ProjectSource, BulkDependencyIncrementer> candidates = updates.findAll {!failedBeforeUpdate.contains(it.key)}
        candidates.each {
           it.value.increment()
        }
        BuildRunner br2 = new BuildRunner(projectSources: candidates.keySet())
        br2.start(4)
        List<BuildRecord> resultsAfterUpgrade = br2.completeBuildRecords
        def failedAfterUpdate = resultsAfterUpgrade.findAll {it.result == BuildRecord.BuildResult.Failed}.collect {it.projectSource}
        println "Failed after update: "+failedAfterUpdate
        failedAfterUpdate.each {
            updates[it].rollback()
        }
    }



    static updateOneProject(SourceRepository repository, String projectName) {
        repository.downloadAll()
        Graph g = new Graph(repository)
        Graph dependents = new Graph(new SpanningTreeBuilder(world: g, treeRoot: projectName).connectedProjects)
        dependents.initRank()

        BuildRunner br = new BuildRunner(projectSources: dependents.nodes.collect {it.projectSource} )
        br.start(4)
        def results = br.completeBuildRecords
        results.findAll { it.result == BuildRecord.BuildResult.Failed }.each { currentBuild ->
            dependents.nodes.find {it.name == currentBuild.projectSource.name }.buildFailed = true
        }

        def rank= 2
        Collection<Node> levelProjects = dependents.nodes.findAll {it.rank == rank && !it.buildFailed}
        Map<Node, BulkDependencyIncrementer> incrementers = new HashMap<>().withDefault {node -> new BulkDependencyIncrementer(node: node)}
        levelProjects.each {incrementers[it].increment()}
        BuildRunner br2 = new BuildRunner(projectSources: levelProjects.collect {it.projectSource} )
        br2.start(4)
        def results2 = br2.completeBuildRecords
        results2.findAll { it.result == BuildRecord.BuildResult.Failed }.each { currentBuild ->
            Node failed = levelProjects.find {it.name == currentBuild.projectSource.name }
            failed.outgoing.each {it.updateFailed = true}
            incrementers[failed].rollback()
        }

       // dependents.nodes.find {it.name == 'dealerautoreports-commons-acceptance'}.buildFailed = true

        def gv = new GraphVizGenerator(graph: dependents)
        gv.generate()
        gv.reveal()

    }


    static buildAll(projects) {
        long start = System.currentTimeMillis()
        BuildRunner br = new BuildRunner(projectSources: projects)
        br.start(8)
        def results = br.completeBuildRecords
        long stop = System.currentTimeMillis()
        results.findAll { it.result == BuildRecord.BuildResult.Failed }.each {
            println it.projectSource.name + " " + it.result
        }

        println "Total time (ms): " + (stop - start)
    }

    static drawGraph(repository) {
        Graph g = new Graph(repository)
        g.initRank()
        g.cycles.each {
            println it
        }
        def gv = new GraphVizGenerator(graph: g)
        gv.generate()
        gv.reveal()
    }


}
