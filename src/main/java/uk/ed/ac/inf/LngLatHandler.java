package uk.ed.ac.inf;

import uk.ac.ed.inf.ilp.data.LngLat;
import uk.ac.ed.inf.ilp.data.NamedRegion;
import uk.ac.ed.inf.ilp.interfaces.LngLatHandling;

public class LngLatHandler implements LngLatHandling {
    /**
     * @param startPosition 
     * @param endPosition
     * @return
     */
    public double distanceTo(LngLat startPosition, LngLat endPosition) {
        return 0;
    }

    /**
     * @param startPosition 
     * @param otherPosition
     * @return
     */
    public boolean isCloseTo(LngLat startPosition, LngLat otherPosition) {
        return false;
    }

    /**
     * @param position 
     * @param region
     * @return
     */
    public boolean isInRegion(LngLat position, NamedRegion region) {
        return false;
    }

    /**
     * @param startPosition 
     * @param angle
     * @return
     */
    public LngLat nextPosition(LngLat startPosition, double angle) {
        return null;
    }
}
