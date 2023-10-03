package uk.ed.ac.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;

import java.awt.geom.Line2D;

public class LngLatHandler implements LngLatHandling {
    /**
     * Returns the Euclidean distance between the provided points
     *
     * @param startPosition: The initial point (usually the drone)
     * @param endPosition: The end point (usually the drone's intended destination)
     * @return: The distance between startPosition and endPosition
     */
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        return Math.sqrt(Math.pow(startPosition.lng() - endPosition.lng(), 2) + Math.pow(startPosition.lat() - endPosition.lat(), 2));
    }

    /**
     * Check whether two positions are considered "close", i.e. within 0.00015 degrees of one another
     *
     * @param startPosition: The initial position (usually the drone)
     * @param otherPosition: The point we want to check proximity to
     * @return: Whether the two points are "close"
     */
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return distanceTo(startPosition, otherPosition) < 0.00015;
    }

    /**
     * Uses the Ray casting algorithm for finding a point in a polygon
     *
     * Given a point `position` and a polygon `region`, we can draw a line
     * from the point beyond the bounding box of the polygon
     *
     * If this line crosses an even number of edges the point is not within the polygon,
     * where if it crosses an odd number it is
     *
     * @param position: The lng, lat position to be checked
     * @param region: The polygon to check against
     * @return: Whether `position` lies inside `region`
     */
    public boolean isInRegion(LngLat position, NamedRegion region) {
        int numCrossings = 0;

        // This is the point directly north of `position` on the north edge of
        // region's bounding rectangle
        LngLat northern = new LngLat(northernBoundaryLng(region), position.lat());

        // The line for checking intersections
        Line2D intersectLine = new Line2D.Double(
                position.lat(), northern.lat(),
                position.lng(), northern.lat()
        );
        // Stores the current edge to be checked
        Line2D vertexEdge;

        // For each vertex in the list, create an edge pairing that vertex with the next one
        for (int n = 0; n < region.vertices().length; n++) {
            vertexEdge = new Line2D.Double(
                    region.vertices()[n].lat(), region.vertices()[n+1].lat(),
                    region.vertices()[n].lng(), region.vertices()[n+1].lng()
            );

            // If the intersectLine intersects the vertexEdge mark down an extra crossing
            if (vertexEdge.intersectsLine(intersectLine)) {
                numCrossings++;
            }
        }

        // If the number of crossings is odd, then the point lies in the region
        return numCrossings % 2 == 1;
    }

    /**
     * Given a polygon `region`, determine the longitude of its northernmost point
     *
     * @param region: The polygon
     * @return: The northernmost longitude
     */
    private double northernBoundaryLng(NamedRegion region) {
        double max = 0;

        // Iterate over the list of vertices to determine what the northernmost point is
        for (LngLat pos : region.vertices()) {
            if (pos.lng() > max) {
                max = pos.lng();
            }
        }

        return max;
    }

    /**
     * Produces the next position when given a point and an angle
     *
     * Includes special case for hovering when angle is set to 999
     *
     * @param startPosition: The start position
     * @param angle: Angle to move along, in degrees
     * @return: The updated position
     */
    public LngLat nextPosition(LngLat startPosition, double angle) {
        // Tells the drone to hover in place when provided the special value
        if (angle == 999) {return startPosition;}

        // The differences in latitude and longitude between the start and end
        // Angles provided are in degrees, but Math.sin() and Math.cos() use radians
        double dLng = 0.00015 * Math.sin(angle * Math.PI / 180);
        double dLat = 0.00015 * Math.cos(angle * Math.PI / 180);

        return new LngLat(startPosition.lng() + dLng, startPosition.lat() + dLat);
    }
}
