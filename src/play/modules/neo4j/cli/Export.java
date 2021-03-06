/**
 * This file is part of logisima-play-neo4j.
 *
 * logisima-play-neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logisima-play-neo4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with logisima-play-neo4j. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @See https://github.com/sim51/logisima-play-neo4j
 */
package play.modules.neo4j.cli;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import play.Play;
import play.db.DBPlugin;
import play.db.jpa.JPAPlugin;
import play.modules.neo4j.cli.export.YmlNode;
import play.modules.neo4j.cli.export.YmlRelation;
import play.modules.neo4j.util.Neo4j;

public class Export {

    public static Map<String, YmlNode>     nodes     = new HashMap<String, YmlNode>();

    public static Map<String, YmlRelation> relations = new HashMap<String, YmlRelation>();

    /**
     * Export YML file method !
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // we retrieve parameters
        String filename = "data";
        String output = "conf/";
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (args[i].startsWith("--filename=")) {
                    filename = args[i].substring(11);
                }
                if (args[i].startsWith("--output=")) {
                    output = args[i].substring(9);
                }
            }
        }

        // initiate play! framework
        File root = new File(System.getProperty("application.path"));
        Play.init(root, System.getProperty("play.id", ""));
        Thread.currentThread().setContextClassLoader(Play.classloader);
        Class c = Play.classloader.loadClass("play.modules.neo4j.cli.Export");
        Method m = c.getMethod("mainWork", String.class, String.class);
        m.invoke(c.newInstance(), filename, output);
        System.exit(0);
    }

    public static void mainWork(String filename, String output) throws Exception {
        new DBPlugin().onApplicationStart();
        new JPAPlugin().onApplicationStart();

        // initiate DB
        Neo4j.initialize();
        // for all node
        for (Node node : Neo4j.db().getAllNodes()) {
            if (node.hasRelationship(Direction.INCOMING) && !node.hasProperty("CLASSNAME")
                    && !node.hasProperty("KEY_COUNTER")) {
                System.out.println("Adding node " + node.getId());
                YmlNode ymlNode = new YmlNode(node);
                nodes.put(ymlNode.id, ymlNode);
                for (Relationship relation : node.getRelationships(Direction.OUTGOING)) {
                    YmlRelation ymlRelation = new YmlRelation(relation);
                    relations.put(ymlRelation.id, ymlRelation);
                }
            }
        }
        writeFile(filename, output);
        System.out.println("End of mainWork");
    }

    /**
     * Method that generate the YLM file.
     * 
     * @param output
     * @param filename
     * @param myHash
     * @throws IOException
     */
    private static void writeFile(String filename, String output) throws IOException {
        // we create the file
        File file = new File(output + "/" + filename + ".yml");
        FileOutputStream fop = new FileOutputStream(file);
        fop.write("# Generated by logisima-play-neo4j (http://github.com/sim51/logisima-play-neo4j).\n".getBytes());
        fop.write("# This module is a part of LogiSima (http://www.logisima.com).\n".getBytes());
        // We write all nodes !
        for (YmlNode ymlNode : nodes.values()) {
            System.out.println("Generating node " + ymlNode.id);
            fop.write(ymlNode.toYml().getBytes());
        }
        // We write all relation !
        for (YmlRelation ymlRelation : relations.values()) {
            System.out.println("Generating relation " + ymlRelation.id);
            fop.write(ymlRelation.toYml().getBytes());
        }
        fop.flush();
        fop.close();
    }
}
