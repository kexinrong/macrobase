package macrobase.util.asap;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataSources {
    public static Map<Integer, String> TABLE_NAMES = ImmutableMap.<Integer, String>builder()
            .put(1, "iowa_liquor").put(2, "nyc_taxi").put(3, "boston_taxi_pickups")
            .put(4, "fridge_data").put(5, "campaign_expenditures").put(6, "vmware_data")
            .put(7, "google_network").put(8, "art_daily_jumpsdown").put(9, "TravelTime_387").build();
    public static Map<Integer, Integer> WINDOW_RANGES = ImmutableMap.<Integer, Integer>builder()
            .put(1, 180 * 24 * 3600 * 1000).put(2, 180 * 24 * 3600 * 1000)
            .put(3, 180 * 24 * 3600 * 1000).put(4, 60 * 24 * 3600 * 1000)
            .put(5, 180 * 24 * 3600 * 1000).put(6, 80 * 24 * 3600 * 1000)
            .put(7, 7 * 24 * 3600 * 1000).put(8, 14 * 24 * 3600 * 1000)
            .put(9, 60 * 24 * 3600 * 1000).build();
    public static Map<Integer, List<String>> COLUMN_NAMES = ImmutableMap.<Integer, List<String>>builder()
            .put(1, new ArrayList<>(Arrays.asList("date","total")))
            .put(2, new ArrayList<>(Arrays.asList("pickup_datetime","passenger_count")))
            .put(3, new ArrayList<>(Arrays.asList("pickup_time", "NULL")))
            .put(4, new ArrayList<>(Arrays.asList("timestamp","reading")))
            .put(5, new ArrayList<>(Arrays.asList("contb_receipt_dt", "contb_receipt_amt")))
            .put(6, new ArrayList<>(Arrays.asList("timestamp","value")))
            .put(7, new ArrayList<>(Arrays.asList("date", "value")))
            .put(8, new ArrayList<>(Arrays.asList("timestamp","value")))
            .put(9, new ArrayList<>(Arrays.asList("timestamp","value"))).build();
    public static Map<Integer, Integer> BIN_SIZE = ImmutableMap.<Integer, Integer>builder()
            .put(1, 3600000).put(2, 3600000).put(3, 3600000).put(4, 3600000)
            .put(5, 3600000).put(6, 3600000).put(7, 600000).put(8, 3600000)
            .put(9, 3600000).build();
}
