package ingprompt.patricia.location.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class GeoPointTest {

    @Test
    void validCoordinates() {
        GeoPoint point = new GeoPoint(4.6097, -74.0817);
        assertEquals(4.6097, point.latitude());
        assertEquals(-74.0817, point.longitude());
    }

    @Test
    void boundaryLatitudeValues() {
        assertDoesNotThrow(() -> new GeoPoint(90, 0));
        assertDoesNotThrow(() -> new GeoPoint(-90, 0));
    }

    @Test
    void boundaryLongitudeValues() {
        assertDoesNotThrow(() -> new GeoPoint(0, 180));
        assertDoesNotThrow(() -> new GeoPoint(0, -180));
    }

    @Test
    void origin() {
        GeoPoint origin = new GeoPoint(0, 0);
        assertEquals(0, origin.latitude());
        assertEquals(0, origin.longitude());
    }

    @ParameterizedTest
    @CsvSource({
            "91, 0",
            "-91, 0",
            "100, 0",
            "-100, 0"
    })
    void latitudeOutOfRange(double lat, double lng) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> new GeoPoint(lat, lng));
        assertTrue(ex.getMessage().contains("latitude out of range"));
    }

    @ParameterizedTest
    @CsvSource({
            "0, 181",
            "0, -181",
            "0, 360",
            "0, -360"
    })
    void longitudeOutOfRange(double lat, double lng) {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> new GeoPoint(lat, lng));
        assertTrue(ex.getMessage().contains("longitude out of range"));
    }

    @Test
    void equalityAndHashCode() {
        GeoPoint a = new GeoPoint(4.6097, -74.0817);
        GeoPoint b = new GeoPoint(4.6097, -74.0817);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void inequalityOnDifferentCoords() {
        GeoPoint a = new GeoPoint(4.6097, -74.0817);
        GeoPoint b = new GeoPoint(40.7128, -74.0060);
        assertNotEquals(a, b);
    }
}
