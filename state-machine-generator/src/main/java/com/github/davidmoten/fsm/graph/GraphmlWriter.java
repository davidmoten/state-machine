package com.github.davidmoten.fsm.graph;

import static org.apache.commons.lang3.StringEscapeUtils.escapeXml10;

import java.io.PrintWriter;

public class GraphmlWriter {

    public void printGraphml(PrintWriter out, Graph graph) {

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"  \n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xmlns:y=\"http://www.yworks.com/xml/graphml\"\n"
                + "    xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns\n"
                + "     http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">");

        out.println("  <key for=\"node\" id=\"d1\" yfiles.type=\"nodegraphics\"/>");
        out.println("  <graph id=\"G\" edgedefault=\"directed\">");

        for (GraphNode node : graph.getNodes()) {
            printNode(out, node.name(), node.isEnabled());
        }

        for (GraphEdge edge : graph.getEdges()) {
            printEdge(out, edge.getFrom().name(), edge.getTo().name());
        }

        out.println("  </graph>");
        out.println("</graphml>");

        out.close();

    }

    private static void printEdge(PrintWriter out, String userId, String id) {
        out.println("    <edge source=\"" + escapeXml10(userId) + "\" target=\"" + escapeXml10(id)
                + "\"/>");
    }

    private static void printNode(PrintWriter out, String userId, boolean enabled) {

        boolean isEmail = userId.contains("@");
        String fillColor;
        if (isEmail)
            fillColor = "#80FF00";
        else if (!enabled)
            fillColor = "#FF2A00";
        else
            fillColor = "#FFCC00";
        out.println("    <node id=\"" + escapeXml10(userId) + "\">");
        out.println("      <data key=\"d1\">");
        out.println("        <y:ShapeNode>");
        out.println(
                "          <y:Geometry height=\"100.0\" width=\"100.0\" x=\"77.0\" y=\"113.0\"/>\n"
                        + "          <y:Fill color=\"" + fillColor + "\" transparent=\"false\"/>\n"
                        + "          <y:BorderStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>");
        out.println("          <y:NodeLabel>" + escapeXml10(userId) + "</y:NodeLabel>");
        out.println("        </y:ShapeNode>");
        out.println("      </data>");
        out.println("    </node>");
    }

}
