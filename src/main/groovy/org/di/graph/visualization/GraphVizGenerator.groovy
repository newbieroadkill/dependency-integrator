package org.di.graph.visualization

import org.di.graph.Graph


class GraphVizGenerator {
    Graph graph
    File script
    File graphic
    //see: http://www.graphviz.org/doc/info/output.html
    String outputType = "png"
    String dotCommandLocation
    String commandExecutionPrefix
    String commandOpenPrefix

    GraphVizGenerator() {
        script = new File(System.getProperty("java.io.tmpdir")+"/di.dot")
        script.delete()
        graphic = new File(System.getProperty("java.io.tmpdir")+"/di.${outputType}")
        graphic.delete()
        if (System.getProperty("os.name").startsWith("Windows")) {
           dotCommandLocation = "C:\\cfx)\\graphviz\\bin\\dot.exe"
            commandExecutionPrefix = "cmd /c "
            commandOpenPrefix = ""
        } else {
            dotCommandLocation = "/usr/local/bin/dot"
            commandExecutionPrefix = ""
            commandOpenPrefix = "open "
        }
    }

    def generate() {
        graph.initRank()
        String content = "digraph G { ranksep=3; nodesep=0.1; node [shape=point,width=.75,height=.5,fontsize=5];\n"
        def levels = graph.nodes.groupBy {it.rank}.keySet().sort{-it}
        content += "  {  node [shape=none]; edge [style=invis]; \n"
        content += "    "+levels.join(" -> ")
        content += "; \n"

        content += "  }\n"

        graph.nodes.groupBy {it.rank}.sort{-it.key}.each { rank, rankNodes ->
            content += "   { rank = same; ${rank};"
            content += rankNodes.collect {fix(it.projectSource.name)}.join ("; ")
            content += "   }\n"
        }

        graph.nodes.each { node ->
            node.outgoing.each { dependency ->
                String edgeStyle = ""
                if (dependency.dependency.version.toString() != dependency.to.projectSource.version.toString() ) {
                    edgeStyle =  "[color=red,style=\"setlinewidth(4)\"]"
                    println node.projectSource.name + " depends on "+dependency.to.name + " version "+dependency.dependency.version.toString()+ " ("+dependency.to.projectSource.version.toString()+")"
                }
                if (dependency.cyclic) {
                    edgeStyle = "[color=blue,style=\"setlinewidth(8)\"]"

                }
                content += "   " + fix(node.projectSource.name) + " -> " + fix(dependency.to.name) + " "+edgeStyle+"; \n"
            }
        }
        content += "}"
        script.delete()
        script << content
        def proc = """${commandExecutionPrefix}${dotCommandLocation} -Tpng ${script.absolutePath} -o ${graphic.absolutePath}""".execute()
        proc.waitFor()

    }

    def reveal() {
        "${commandExecutionPrefix}${commandOpenPrefix}${graphic.absolutePath}".execute()
    }

    static String fix(String n) {
        n.replaceAll("-", "_")
    }
}