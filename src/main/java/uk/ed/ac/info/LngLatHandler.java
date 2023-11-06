package uk.ed.ac.info;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;
import uk.ac.ed.inf.ilp.constant.*;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

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
        return distanceTo(startPosition, otherPosition) < SystemConstants.DRONE_IS_CLOSE_DISTANCE;
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
        Path2D polygon = new Path2D.Double();
        polygon.moveTo(region.vertices()[0].lat(), region.vertices()[0].lng());

        for (LngLat vertex : region.vertices()) {
            polygon.lineTo(vertex.lat(), vertex.lng());
        }

        polygon.closePath();

        return polygon.contains(new Point2D.Double(position.lat(), position.lng()));
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
        double dLng = SystemConstants.DRONE_MOVE_DISTANCE * Math.cos(angle * Math.PI/180);
        double dLat = SystemConstants.DRONE_MOVE_DISTANCE * Math.sin(angle * Math.PI/180);

        return new LngLat(startPosition.lng() + dLng, startPosition.lat() + dLat);
    }
}
