package ingprompt.patricia.location.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoPointTest {

    @Test
    void validCoordinates_areAccepted() {
        GeoPoint point = new GeoPoint(4.65, -74.05);
        assertThat(point.latitude()).isEqualTo(4.65);
        assertThat(point.longitude()).isEqualTo(-74.05);
    }

    @Test
    void latitudeOutOfRange_throws() {
        assertThatThrownBy(() -> new GeoPoint(91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(-91, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void longitudeOutOfRange_throws() {
        assertThatThrownBy(() -> new GeoPoint(0, 181)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeoPoint(0, -181)).isInstanceOf(IllegalArgumentException.class);
    }
}
