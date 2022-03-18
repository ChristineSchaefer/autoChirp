package workflowTests;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created by Ricarda Boente on 16.03.2022.
 * simple test to prove that ZonedDateTime automatically implements Daylight-Saving-Time switches
 */
public class SimpleDSTTest {

    @Test
    public void timeTest() {
        // the switch from winter to summer time takes place at 02:00 on 27.03.2022
        LocalDateTime localDateTimeBeforeDST = LocalDateTime.of(2022, 03, 27, 1, 55);
        LocalDateTime localDateTimeAfterDST = LocalDateTime.of(2022, 03, 27, 2, 05);
        // apply Berlin time zone to the two dates
        ZoneId zoneId = ZoneId.of("Europe/Berlin");
        ZonedDateTime zonedDateTimeBeforeDST = localDateTimeBeforeDST.atZone(zoneId);
        Assert.assertEquals(zonedDateTimeBeforeDST.toString(), "2022-03-27T01:55+01:00[Europe/Berlin]");
        ZonedDateTime zonedDateTimeAfterDST = localDateTimeAfterDST.atZone(zoneId);
        Assert.assertEquals(zonedDateTimeAfterDST.toString(), "2022-03-27T03:05+02:00[Europe/Berlin]");
        // ZonedDateTime realizes that between "before" and "after" there are 10 minutes, not 70
        Assert.assertEquals(ChronoUnit.MINUTES.between(zonedDateTimeBeforeDST, zonedDateTimeAfterDST), 10);

        // the switch from summer to winter time takes place at 03:00 on 30.10.2022
        LocalDateTime localDateTimeBeforeDST2 = LocalDateTime.of(2022, 10, 30, 2, 55);
        LocalDateTime localDateTimeAfterDST2 = LocalDateTime.of(2022, 10, 30, 3, 05);
        // apply Berlin time zone to the two dates
        ZonedDateTime zonedDateTimeBeforeDST2 = localDateTimeBeforeDST2.atZone(zoneId);
        Assert.assertEquals(zonedDateTimeBeforeDST2.toString(), "2022-10-30T02:55+02:00[Europe/Berlin]");
        ZonedDateTime zonedDateTimeAfterDST2 = localDateTimeAfterDST2.atZone(zoneId);
        Assert.assertEquals(zonedDateTimeAfterDST2.toString(), "2022-10-30T03:05+01:00[Europe/Berlin]");
        // adding 10 minutes to "before"-date gives a different date than the "after"-date
        ZonedDateTime zonedDateTimeInDST2 = zonedDateTimeBeforeDST2.plus(10, ChronoUnit.MINUTES);
        Assert.assertEquals(zonedDateTimeInDST2.toString(), "2022-10-30T02:05+01:00[Europe/Berlin]");
        // ZonedDateTime realizes that between "before" and "after" there are 70 minutes, not 10
        Assert.assertEquals(ChronoUnit.MINUTES.between(zonedDateTimeBeforeDST2, zonedDateTimeAfterDST2), 70);
    }

}
