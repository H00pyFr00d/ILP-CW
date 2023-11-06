package uk.ed.ac.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import uk.ac.ed.inf.ilp.data.Order;
import uk.ac.ed.inf.ilp.gsonUtils.LocalDateSerializer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutputProcessor {
    Path resultDir;
    public OutputProcessor() {
        // Results are stored in PizzaDronz/resultfiles/, but that directory may not exist by default
        this.resultDir = FileSystems.getDefault().getPath("resultfiles");

        // Creating a directory that already exists throws an IOException
        try {
            Files.createDirectory(resultDir);
            System.out.println("Results directory created");
        }
        // If we see an IOException then, we can just carry on as we know the directory exists
        catch(IOException ignored) {
            System.out.println("Results directory already exists");
        }
    }

    public void writeDeliveries(String filename, Order[] orders) {
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateSerializer()).setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(resultDir.getFileName() + filename)) {
            gson.toJson(orders, writer);
            System.out.println("File created: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeFlightpathJson(String filename, HashMap<String, ArrayList<PathNode>> paths) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(resultDir.getFileName() + filename)) {
            JsonArray flightpath = convertFlightpathToJson(paths);
            gson.toJson(flightpath, writer);
            System.out.println("File created: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePathGeoJson(String filename, HashMap<String, ArrayList<PathNode>> paths) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileWriter writer = new FileWriter(resultDir.getFileName() + filename)) {
            JsonObject geoJson = convertToGeoJson(paths);
            gson.toJson(geoJson, writer);
            System.out.println("File created: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static JsonArray convertFlightpathToJson(HashMap<String, ArrayList<PathNode>> paths) {
        JsonArray totalJson = new JsonArray();

        for (Map.Entry<String, ArrayList<PathNode>> path : paths.entrySet()) {
            for (PathNode node : path.getValue()) {
                JsonObject record = new JsonObject();

                record.addProperty("orderNo", path.getKey());
                record.addProperty("fromLongitude", node.prev().lng());
                record.addProperty("fromLatitude", node.prev().lat());
                record.addProperty("angle", node.angle());
                record.addProperty("fromLongitude", node.curr().lng());
                record.addProperty("fromLatitude", node.curr().lat());

                totalJson.add(record);
            }
        }

        return totalJson;
    }

    private static JsonObject convertToGeoJson(HashMap<String, ArrayList<PathNode>> paths) {
        JsonObject totalJson = new JsonObject();
        JsonArray features = new JsonArray();
        Gson gson = new GsonBuilder().create();

        for (Map.Entry<String, ArrayList<PathNode>> path : paths.entrySet()) {
            JsonObject feature = new JsonObject();
            JsonObject geometry = new JsonObject();
            JsonObject properties = new JsonObject();

            List<JsonArray> pointList = path
                    .getValue()
                    .stream()
                    .map(point -> {
                        JsonArray entry = new JsonArray();
                        entry.add(point.curr().lng());
                        entry.add(point.curr().lat());
                        return entry;
                    })
                    .toList();

            geometry.addProperty("type", "LineString");
            geometry.add("coordinates", gson.toJsonTree(pointList));

            feature.addProperty("type", "Feature");
            feature.add("geometry", geometry);

            feature.add("properties", properties);

            features.add(feature);
        }

        totalJson.addProperty("type", "FeatureCollection");
        totalJson.add("features", features);

        return totalJson;
    }
}
