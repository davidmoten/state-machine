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
        out.println("  <key for=\"edge\" id=\"d2\" attr.name=\"Event\" attr.type=\"string\"/>");
        out.println("  <key for=\"edge\" id=\"d3\" yfiles.type=\"edgegraphics\"/>");
        out.println("  <graph id=\"G\" edgedefault=\"directed\">");

        for (GraphNode node : graph.getNodes()) {
            printNode(out, node.name(), node.descriptionHtml());
        }

        for (GraphEdge edge : graph.getEdges()) {
            printEdge(out, edge.getFrom().name(), edge.getTo().name(), edge.label());
        }

        out.println("  </graph>");
        out.println("</graphml>");

        out.close();

    }

    private static void printEdge(PrintWriter out, String from, String to, String label) {
        out.println("    <edge source=\"" + escapeXml10(from) + "\" target=\"" + escapeXml10(to)
                + "\">");
        out.println("        <data key=\"d2\">" + label + "</data>");
        out.println("<data key=\"d3\">\n" + "        <y:PolyLineEdge>\n"
                + "          <y:Path sx=\"0.0\" sy=\"75.0\" tx=\"-75.0\" ty=\"-0.0\">\n"
                + "            <y:Point x=\"75.0\" y=\"250.0\"/>\n" + "          </y:Path>\n"
                + "          <y:LineStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>\n"
                + "          <y:Arrows source=\"none\" target=\"standard\"/>\n"
                + "          <y:EdgeLabel alignment=\"center\" configuration=\"AutoFlippingLabel\" distance=\"2.0\" fontFamily=\"Dialog\" fontSize=\"12\" fontStyle=\"plain\" hasBackgroundColor=\"false\" hasLineColor=\"false\" height=\"17.96875\" modelName=\"custom\" preferredPlacement=\"anywhere\" ratio=\"0.5\" textColor=\"#000000\" visible=\"true\" width=\"68.822265625\" x=\"-64.4111328125\" y=\"48.0078125\">"
                + label + "<y:LabelModel>\n"
                + "              <y:SmartEdgeLabelModel autoRotationEnabled=\"false\" defaultAngle=\"0.0\" defaultDistance=\"10.0\"/>\n"
                + "            </y:LabelModel>\n" + "            <y:ModelParameter>\n"
                + "              <y:SmartEdgeLabelModelParameter angle=\"0.0\" distance=\"30.0\" distanceToCenter=\"true\" position=\"right\" ratio=\"0.5\" segment=\"0\"/>\n"
                + "            </y:ModelParameter>\n"
                + "            <y:PreferredPlacementDescriptor angle=\"0.0\" angleOffsetOnRightSide=\"0\" angleReference=\"absolute\" angleRotationOnRightSide=\"co\" distance=\"-1.0\" frozen=\"true\" placement=\"anywhere\" side=\"anywhere\" sideReference=\"relative_to_edge_flow\"/>\n"
                + "          </y:EdgeLabel>\n" + "          <y:BendStyle smoothed=\"false\"/>\n"
                + "        </y:PolyLineEdge>\n" + "      </data>");
        out.println("    </edge>");
    }

    private static void printNode(PrintWriter out, String nodeName, String descriptionHtml) {
        String fillColor = "#F3F2C0";
        out.println("    <node id=\"" + escapeXml10(nodeName) + "\">");
        out.println("      <data key=\"d1\">");
        out.println("        <y:ShapeNode>");
        out.println(
                "          <y:Geometry height=\"150.0\" width=\"250.0\" x=\"77.0\" y=\"113.0\"/>\n"
                        + "          <y:Fill color=\"" + fillColor + "\" transparent=\"false\"/>\n"
                        + "          <y:BorderStyle color=\"#000000\" type=\"line\" width=\"1.0\"/>");
        out.println("          <y:NodeLabel>"
                + escapeXml10("<html><p><b>" + nodeName + "</b></p>" + descriptionHtml + "</html>")
                + "</y:NodeLabel>");
        out.println("        </y:ShapeNode>");
        out.println("      </data>");
        out.println("    </node>");
    }

}
