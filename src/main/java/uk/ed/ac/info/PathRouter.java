package uk.ed.ac.info;

import uk.ac.ed.inf.ilp.constant.SystemConstants;
import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;

import java.util.*;

record PathNode(LngLat curr, LngLat prev, Double angle, int step) {}

public class PathRouter {
    private final int ANGLES = 16;
    private final NamedRegion centralArea;
    private final NamedRegion[] noFlyZones;
    private final Double HOVER = 999.0;

    public PathRouter(NamedRegion centralArea, NamedRegion[] noFlyZones) {
        this.centralArea = centralArea;
        this.noFlyZones = noFlyZones;
    }



    /**
     * Uses the A* algorithm to determine the most efficient route from the start to the destination
     * A* uses a function h(pos) which is defined as follows, f(pos) = g(pos) + h(pos)
     *  - g(pos) is the actual distance travelled from start to pos, i.e number of steps * move distance
     *  - h(pos) is the predicted distance between pos and dest, using Euclidean distance as a heuristic
     *
     * @param start: The start point for pathing
     * @param dest: The end point for pathing
     * @return: The optimal list of moves to make to get from start to dest,
     *          as a list of coordinates resulting from taking those steps
     */
    public ArrayList<PathNode> getRoute(LngLat start, LngLat dest) {
        ArrayList<PathNode> visited = new ArrayList<>();
        LngLatHandler lngLatHandler = new LngLatHandler();

        PathNode node = new PathNode(start, null, 999.0, 0);

        visited.add(node);
        HashMap<PathNode, Double> frontier = new HashMap<>(getValidAdjacentPoints(node, dest, visited));

        while (!lngLatHandler.isCloseTo(node.curr(), dest)) {
            node = Collections.min(frontier.entrySet(), Map.Entry.comparingByValue()).getKey();

            visited.add(node);
            frontier.remove(node);

            HashMap<PathNode, Double> newFrontier = getValidAdjacentPoints(node, dest, visited);
            if (newFrontier.size() < ANGLES) {
                frontier.putAll(newFrontier);
            } else {
                Map.Entry<PathNode, Double> minPair = Collections.min(newFrontier.entrySet(), Map.Entry.comparingByValue());
                frontier.put(minPair.getKey(), minPair.getValue());
            }
        }

        ArrayList<PathNode> finalPath = new ArrayList<>();
        finalPath.add(new PathNode(node.curr(), node.curr(), HOVER, node.step() + 1));

        while (node.prev() != null) {
            finalPath.add(node);

            for (PathNode visit : visited) {
                if (visit.curr() == node.prev()) {
                    node = visit;
                    visited.remove(visit);
                    break;
                }
            }
        }

        return finalPath;
    }

    /**
     * Gets all adjacent points which follow the following restrictions:
     * - If a point is in the central region, neighbors outside the region are invalid
     * - Neighbouring points must not be within no-fly zones
     *
     * @param point: The point to determine the valid neighbours of
     * @return: The optimal list of moves to make to get from start to dest,
     * as a list of coordinates resulting from taking those steps
     */
    private HashMap<PathNode, Double> getValidAdjacentPoints(PathNode node, LngLat dest, ArrayList<PathNode> visited) {
        HashMap<PathNode, Double> validAdjacent = new HashMap<>();

        LngLatHandler lngLatHandler = new LngLatHandler();
        boolean inCentral = lngLatHandler.isInCentralArea(node.curr(), centralArea);

        PathNode next;

        for (double angle = 0; angle < 360; angle += 360.0/ANGLES) {
            // Calculate the coordinates of the point along the specified angle
            next = new PathNode(lngLatHandler.nextPosition(node.curr(), angle), node.curr(), angle, node.step() + 1);

            // Ignore any points we've visited before
            if (!visited.contains(next)) {
                // The following truth table shows the desired results:
                //
                // inCentral | nextInCentral | result
                // ----------+---------------+-------
                //     false |         false |   true
                //     false |          true |   true
                //      true |         false |  false
                //      true |          true |   true
                //
                // This is the same as logical implication, so the following works (as A -> B == ¬A v B)
                boolean centralValid = !inCentral || lngLatHandler.isInCentralArea(next.curr(), this.centralArea);

                // Streams require variables to be "effectively final", so this copy needs made
                PathNode finalNext = next;

                // Convert the noFlyZones to a stream, and then use a map to get a stream of booleans which tells us
                // which regions the point is inside (if any)
                //
                // We then apply a reducer, giving true if any of the regions contained the point and false otherwise
                //
                // The orElse(true) ensures that if the list of no-fly zones is empty this still works
                //
                // Finally the result is negated, so we get validity not invalidity
                boolean noFlyValid = !Arrays.stream(noFlyZones)
                        .map(noFlyZone -> lngLatHandler.isInRegion(finalNext.curr(), noFlyZone))
                        .reduce((a, b) -> a || b)
                        .orElse(true);

                if (centralValid && noFlyValid) {
                    double g_value = next.step() * SystemConstants.DRONE_MOVE_DISTANCE;
                    double h_value = lngLatHandler.distanceTo(next.curr(), dest);
                    validAdjacent.put(next, g_value + h_value);
                }
            }
        }
        return validAdjacent;
    }
}
